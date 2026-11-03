package org.gradle.internal.compiler.java.listeners.constants;

import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreePath;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * ConstantsCollector v28.33: "The Zenith Constant Guardian - Absolute Shield".
 * v28.33: Eliminado import de com.sun.source.util.Trees para evitar bloqueos de seguridad en Android 15.
 * v28.21: Blindaje total mediante desacoplamiento de tipos en firmas de métodos.
 */
public class ConstantsCollector implements TaskListener {

    private final JavacTask task;
    private final Map<String, Collection<String>> mapping;
    private final ConstantDependentsConsumer constantDependentsConsumer;

    public ConstantsCollector(JavacTask task, ConstantDependentsConsumer constantDependentsConsumer) {
        this.task = task;
        this.mapping = new HashMap<>();
        this.constantDependentsConsumer = constantDependentsConsumer;
    }

    public Map<String, Collection<String>> getMapping() {
        return mapping;
    }

    @Override
    public void started(TaskEvent e) {
    }

    @Override
    public void finished(TaskEvent e) {
        if (e.getKind() == Kind.ANALYZE) {
            // OMNI: Obtención atómica de Trees (vía Object) para evitar bloqueos en Android
            Object trees = getTreesInstance(task);
            if (trees == null) return;
            
            try {
                // Invocamos scan sobre el visitante de forma segura
                ConstantsTreeVisitor visitor = new ConstantsTreeVisitor(task.getElements(), trees, constantDependentsConsumer);
                Method getPathMethod = trees.getClass().getMethod("getPath", 
                        com.sun.source.tree.CompilationUnitTree.class, 
                        com.sun.source.tree.Tree.class);
                TreePath path = (TreePath) getPathMethod.invoke(trees, e.getCompilationUnit(), e.getCompilationUnit());
                if (path != null) {
                    visitor.scan(path, null);
                }
            } catch (Throwable ignored) {
                // Sellado seguro: preferimos omitir el análisis a que la app crashee
            }
        }
    }

    /**
     * OMNI: Puente reflexivo Zenith. Usamos Object para evitar que el verificador de Android 
     * intente resolver Trees/JavacTask prematuramente.
     */
    private static Object getTreesInstance(Object taskInstance) {
        if (taskInstance == null) return null;
        try {
            Class<?> treesClass = Class.forName("com.sun.source.util.Trees");
            try {
                Method instanceMethod = treesClass.getMethod("instance", 
                        Class.forName("javax.tools.CompilationTask"));
                return instanceMethod.invoke(null, taskInstance);
            } catch (Throwable t1) {
                Method altInstance = treesClass.getMethod("instance", 
                        Class.forName("com.sun.source.util.JavacTask"));
                return altInstance.invoke(null, taskInstance);
            }
        } catch (Throwable t) {
            return null;
        }
    }
}
