package com.tyron.completion.java.compiler;

import com.sun.source.util.Trees;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.JavacTask;
import java.lang.reflect.Method;

public class ReflectionUtil {
    
    private static Object processingEnvironment;
    
    public static void setProcessingEnvironment(Object env) {
        processingEnvironment = env;
    }
    
    public static Object getProcessingEnvironment() {
        return processingEnvironment;
    }
    
    @SuppressWarnings("unchecked")
    public static Trees getTreesInstance(Object task) {
        try {
            if (task == null) return null;
            
            Class<?> treesClass = Class.forName("com.sun.source.util.Trees");
            
            // Try to find the instance(CompilationTask) method
            // We iterate through interfaces to find a match for JavacTask or CompilationTask
            for (Class<?> iface : task.getClass().getInterfaces()) {
                try {
                    Method instanceMethod = treesClass.getMethod("instance", iface);
                    return (Trees) instanceMethod.invoke(null, task);
                } catch (NoSuchMethodException ignored) {
                }
            }
            
            // Fallback to searching the class hierarchy if interfaces didn't work
            Class<?> current = task.getClass();
            while (current != null && current != Object.class) {
                try {
                    Method instanceMethod = treesClass.getMethod("instance", current);
                    return (Trees) instanceMethod.invoke(null, task);
                } catch (NoSuchMethodException ignored) {
                }
                current = current.getSuperclass();
            }

            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static SourcePositions getSourcePositions(Object task) {
        Trees trees = getTreesInstance(task);
        if (trees != null) {
            return trees.getSourcePositions();
        }
        return null;
    }

    public static Object getJavacTreesInstance(Object context) {
        try {
            if (context == null) return null;
            
            Class<?> treesClass = Class.forName("com.sun.source.util.Trees");
            Method instanceMethod = treesClass.getMethod("instance", context.getClass());
            return instanceMethod.invoke(null, context);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
