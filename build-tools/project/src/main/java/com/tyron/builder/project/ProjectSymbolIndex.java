package com.tyron.builder.project;

import android.util.Log;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.builder.project.api.Module;
import com.tyron.builder.project.impl.AndroidModuleImpl;
import java.io.File;
import java.util.*;

/**
 * ProjectSymbolIndex v44.25.10: "The Omni-Scanner - Neural Resonance".
 * v44.25.10: Indexación Híbrida - Soporte para símbolos dinámicos en build/gen y generated/.
 * v44.25.10: Priorización de FQN para evitar colisiones entre recursos de librerías.
 */
public class ProjectSymbolIndex {

    private final Map<String, Set<String>> mModuleSymbols = new HashMap<>();

    public void indexProject(Project project) {
        Log.d("OMNI_INDEX", "🚀 Iniciando Escaneo Neural de Símbolos...");
        for (Module module : project.getModules()) {
            indexModule(module);
        }
    }

    public void indexModule(Module module) {
        Set<String> symbols = new HashSet<>();
        
        // 1. Símbolos de Código (Java/Kotlin)
        if (module instanceof com.tyron.builder.project.api.JavaModule) {
            symbols.addAll(((com.tyron.builder.project.api.JavaModule) module).getJavaFiles().keySet());
        }

        if (module instanceof AndroidModule) {
            AndroidModule androidModule = (AndroidModule) module;
            
            // Forzar re-indexación del módulo para capturar cambios en build/gen
            if (androidModule instanceof AndroidModuleImpl) {
                ((AndroidModuleImpl) androidModule).index();
            }

            // Capturar fuentes Kotlin
            symbols.addAll(androidModule.getKotlinFiles().keySet());
            
            // 2. Símbolos Dinámicos (R.java y ViewBinding)
            // Extraemos los símbolos de las clases de recursos indexadas en AndroidModuleImpl
            symbols.addAll(androidModule.getResourceClasses().keySet());
            
            Log.d("OMNI_INDEX", "📡 Sincronía Estructural en " + module.getName() + ": " + symbols.size() + " neuronas activas.");
        }

        mModuleSymbols.put(module.getName(), symbols);
    }

    /**
     * Busca un nombre cualificado completo (FQN) con heurística de relevancia.
     */
    public String findFqnFor(String symbolName) {
        if (symbolName == null || symbolName.isEmpty()) return null;

        String bestMatch = null;
        for (Set<String> symbols : mModuleSymbols.values()) {
            for (String fqn : symbols) {
                // Si el FQN termina exactamente con el nombre de la clase
                if (fqn.endsWith("." + symbolName)) {
                    // Priorizar el paquete del proyecto sobre el de librerías AndroidX
                    if (bestMatch == null || !fqn.contains("androidx.")) {
                        bestMatch = fqn;
                    }
                } else if (fqn.equals(symbolName)) {
                    return fqn;
                }
            }
        }
        return bestMatch;
    }

    public Set<String> getSymbolsForModule(String moduleName) {
        return mModuleSymbols.getOrDefault(moduleName, Collections.emptySet());
    }
}
