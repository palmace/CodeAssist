package com.tyron.builder.project.impl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.tyron.builder.model.CodeAssistLibrary;
import com.tyron.builder.project.api.JavaModule;
import com.tyron.builder.project.util.PackageTrie;
import com.tyron.common.util.StringSearch;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JavaModuleImpl extends ModuleImpl implements JavaModule {

    private final Map<String, File> mClassFiles;
    private final Map<String, File> mJavaFiles;
    private final Map<String, CodeAssistLibrary> mLibraryHashMap;
    private final Map<String, File> mInjectedClassesMap;
    private final Set<File> mLibraries;
    private final PackageTrie mClassIndex = new PackageTrie();

    public JavaModuleImpl(File root) {
        super(root);
        mJavaFiles = new HashMap<>();
        mClassFiles = new HashMap<>();
        mLibraries = new HashSet<>();
        mInjectedClassesMap = new HashMap<>();
        mLibraryHashMap = new HashMap<>();
    }

    @Override
    public File getBuildDirectory() {
        return new File(getRootFile(), "build");
    }

    @NonNull
    @Override
    public PackageTrie getClassIndex() { return mClassIndex; }

    @NonNull
    @Override
    public Map<String, File> getJavaFiles() { return mJavaFiles; }

    @Nullable
    @Override
    public File getJavaFile(@NonNull String packageName) { return mJavaFiles.get(packageName); }

    @Override
    public void removeJavaFile(@NonNull String packageName) {
        mJavaFiles.remove(packageName);
        mClassIndex.remove(packageName);
    }

    @Override
    public void addJavaFile(@NonNull File javaFile) {
        if (!javaFile.getName().endsWith(".java")) return;
        String className = getFullyQualifiedName(javaFile);
        mJavaFiles.put(className, javaFile);
        mClassIndex.add(className);
    }

    @Override
    public void putLibraryHashes(Map<String, CodeAssistLibrary> hashes) { mLibraryHashMap.putAll(hashes); }

    @Nullable
    @Override
    public CodeAssistLibrary getLibrary(String hash) { return mLibraryHashMap.get(hash); }

    @Override
    public Set<String> getAllClasses() {
        Set<String> classes = new HashSet<>();
        classes.addAll(mJavaFiles.keySet());
        classes.addAll(mClassFiles.keySet());
        classes.addAll(mInjectedClassesMap.keySet());
        return classes;
    }

    @Override
    public List<File> getLibraries() { return ImmutableList.copyOf(mLibraries); }

    @Override
    public void addLibrary(@NonNull CodeAssistLibrary library) {
        File file = library.getSourceFile();
        if (file == null) return;
        
        // CORRECCIÓN v32.9: Aceptar todo lo que parezca una librería para no perder dependencias
        String name = file.getName().toLowerCase();
        if (name.endsWith(".jar") || name.endsWith(".aar") || name.contains("classes")) {
            mLibraries.add(file);
            if (name.endsWith(".jar")) {
                try { putJar(file); } catch (IOException ignore) {}
            }
        }
    }

    protected void putJar(File file) throws IOException {
        if (file == null) return;
        try (JarFile jar = new JarFile(file)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class") || entry.getName().contains("$")) continue;
                String packageName = entry.getName().replace("/", ".").substring(0, entry.getName().length() - ".class".length());
                mClassFiles.put(packageName, file);
                mClassIndex.add(packageName);
            }
        }
    }

    @NonNull
    @Override
    public File getResourcesDir() { return new File(getRootFile(), "src/main/resources"); }

    @NonNull
    @Override
    public File getJavaDirectory() { return new File(getRootFile(), "src/main/java"); }

    @Override
    public File getLibraryDirectory() { return new File(getRootFile(), "libs"); }

    @Override
    public File getLibraryFile() { return new File(getRootFile(), "libraries.json"); }

    @Override
    public File getLambdaStubsJarFile() {
        try {
            Class<?> clazz = Class.forName("com.tyron.builder.BuildModule");
            Method m = clazz.getDeclaredMethod("getLambdaStubs");
            return (File) m.invoke(null);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public File getBootstrapJarFile() {
        try {
            Class<?> clazz = Class.forName("com.tyron.builder.BuildModule");
            Method m = clazz.getDeclaredMethod("getAndroidJar");
            return (File) m.invoke(null);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Map<String, File> getInjectedClasses() { return ImmutableMap.copyOf(mInjectedClassesMap); }

    @Override
    public void addInjectedClass(@NonNull File javaFile) {
        if (!javaFile.getName().endsWith(".java")) return;
        mInjectedClassesMap.put(getFullyQualifiedName(javaFile), javaFile);
    }

    private static String getFullyQualifiedName(@NonNull File javaFile) {
        String packageName = StringSearch.packageName(javaFile);
        return (packageName == null ? "" : packageName + ".") + javaFile.getName().replace(".java", "");
    }

    @Override public void open() throws IOException { super.open(); }
    @Override public void index() {}
    @Override public void clear() { mJavaFiles.clear(); mLibraries.clear(); mLibraryHashMap.clear(); }
}
