package com.tyron.code.util;

import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.Module;
import com.tyron.builder.project.impl.AndroidModuleImpl;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ProjectSymbolIndex v1.1: "The Navigator's Map".
 * v1.1: Añadido soporte para actualización incremental de símbolos (addSymbol).
 */
public class ProjectSymbolIndex {

    private final Map<String, String> mClassNameToFqn = new ConcurrentHashMap<>();

    /**
     * Indexación completa del proyecto.
     */
    public void indexProject(Project project) {
        if (project == null) return;
        mClassNameToFqn.clear();

        for (Module module : project.getModules()) {
            if (module instanceof AndroidModuleImpl) {
                AndroidModuleImpl am = (AndroidModuleImpl) module;
                
                // Indexar archivos Kotlin
                for (String fqn : am.getKotlinFiles().keySet()) {
                    addSymbol(fqn, null);
                }
                
                // Indexar archivos Java
                for (String fqn : am.getAllClasses()) {
                    addSymbol(fqn, null);
                }
            }
        }
    }

    /**
     * Añade un símbolo al índice de forma incremental.
     */
    public void addSymbol(String fqn, File file) {
        if (fqn == null || fqn.isEmpty()) return;
        String simpleName = fqn.contains(".") ? fqn.substring(fqn.lastIndexOf('.') + 1) : fqn;
        mClassNameToFqn.put(simpleName, fqn);
    }

    /**
     * Busca el FQN (Full Qualified Name) dado un nombre simple.
     */
    public String findFqnFor(String simpleName) {
        if (simpleName == null) return null;
        return mClassNameToFqn.get(simpleName);
    }
}
