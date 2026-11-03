package org.gradle.internal.classloader;

import org.gradle.internal.Cast;
import org.gradle.internal.Factory;
import org.gradle.internal.classpath.ClassPath;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VisitableURLClassLoader extends URLClassLoader implements ClassLoaderHierarchy {
    static {
        try {
            ClassLoader.registerAsParallelCapable();
        } catch (NoSuchMethodError ignore) {
            // Not supported on Java 6
        }
    }

    private final Map<Object, Object> userData = new HashMap<Object, Object>();

    public synchronized <T> T getUserData(Object consumerId, Factory<T> onMiss) {
        if (userData.containsKey(consumerId)) {
            return Cast.uncheckedCast(userData.get(consumerId));
        }
        T value = onMiss.create();
        userData.put(consumerId, value);
        return value;
    }

    private final String name;
    private Object dexLoader;
    private Method findClassMethod;
    private Method addDexPathMethod;
    private Method getResourceMethod;

    public VisitableURLClassLoader(String name, ClassLoader parent, Collection<URL> urls) {
        this(name, urls.toArray(new URL[0]), parent);
    }

    public VisitableURLClassLoader(String name, ClassLoader parent, ClassPath classPath) {
        this(name, classPath.getAsURLArray(), parent);
    }

    private VisitableURLClassLoader(String name, URL[] classpath, ClassLoader parent) {
        super(classpath, parent);
        this.name = name;
        
        initDexLoader(name, classpath, parent);
    }

    private void initDexLoader(String name, URL[] classpath, ClassLoader parent) {
        try {
            Class<?> dexLoaderClass = Class.forName("com.tyron.groovy.DexBackedURLClassLoader");
            Constructor<?> constructor = dexLoaderClass.getConstructor(String.class, ClassLoader.class, ClassPath.class);
            
            List<java.io.File> files = new ArrayList<java.io.File>();
            for (URL url : classpath) {
                if ("file".equals(url.getProtocol())) {
                    files.add(new java.io.File(url.getPath()));
                }
            }
            
            ClassPath cp = org.gradle.internal.classpath.DefaultClassPath.of(files);
            this.dexLoader = constructor.newInstance(name, parent, cp);
            this.findClassMethod = dexLoaderClass.getMethod("findClass", String.class);
            this.addDexPathMethod = dexLoaderClass.getMethod("addDexPathPublic", String.class);
            this.getResourceMethod = dexLoaderClass.getMethod("getResource", String.class);
        } catch (Throwable t) {
            // Fallback or ignore if not on Android/not available
        }
    }

    /**
     * Static factory method required by newer Gradle Tooling API versions.
     */
    public static VisitableURLClassLoader fromClassPath(String name, ClassLoader parent, ClassPath classPath) {
        return new VisitableURLClassLoader(name, parent, classPath);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (dexLoader != null && findClassMethod != null) {
            try {
                return (Class<?>) findClassMethod.invoke(dexLoader, name);
            } catch (Throwable ignored) {
            }
        }

        try {
            return super.findClass(name);
        } catch (ClassNotFoundException e) {
            throw e;
        } catch (Throwable t) {
            String message = "Safe-guard: Could not load class " + name + " on Android";
            ClassNotFoundException cnfe = new ClassNotFoundException(message);
            try {
                cnfe.initCause(t);
            } catch (Throwable ignored) {
            }
            throw cnfe;
        }
    }

    @Override
    public URL getResource(String name) {
        URL resource = super.getResource(name);
        if (resource != null) {
            return resource;
        }
        if (dexLoader != null && getResourceMethod != null) {
            try {
                return (URL) getResourceMethod.invoke(dexLoader, name);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
        if (dexLoader != null && addDexPathMethod != null) {
            if ("file".equals(url.getProtocol())) {
                try {
                    addDexPathMethod.invoke(dexLoader, new java.io.File(url.getPath()).getAbsolutePath());
                } catch (Throwable ignored) {
                }
            }
        }
    }

    @Override
    public String toString() {
        return VisitableURLClassLoader.class.getSimpleName() + "(" + name + ")";
    }

    @Override
    public void visit(ClassLoaderVisitor visitor) {
        URL[] urls = getURLs();
        visitor.visitSpec(new Spec(name, Arrays.asList(urls)));
        visitor.visitClassPath(urls);
        visitor.visitParent(getParent());
    }

    public static class Spec extends ClassLoaderSpec {
        final String name;
        final List<URL> classpath;

        public String getName() {
            return name;
        }

        public Spec(String name, List<URL> classpath) {
            this.name = name;
            this.classpath = classpath;
        }

        public List<URL> getClasspath() {
            return classpath;
        }

        @Override
        public String toString() {
            return "{url-class-loader name:" + name + ", classpath:" + classpath + "}";
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != getClass()) {
                return false;
            }
            Spec other = (Spec) obj;
            return classpath.equals(other.classpath);
        }

        @Override
        public int hashCode() {
            return classpath.hashCode();
        }
    }
}
