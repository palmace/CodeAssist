package com.tyron.builder.compiler.java;

import android.util.Log;
import com.tyron.builder.BuildModule;
import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.builder.project.api.JavaModule;
import com.tyron.builder.project.impl.AndroidModuleImpl;
import com.tyron.common.util.Decompress;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Library Guard v44.25.6: "The Sovereign Guardian - Adaptive Resonance".
 * v44.25.6: Sistema de Rescate Heurístico para evitar fallos de suministro si las librerías existen en el arsenal.
 * v44.25.5: Rescate Crítico de VectorDrawable para evitar NoClassDefFoundError.
 */
public class CheckLibrariesTask extends Task<JavaModule> {

    private static final String VERSION = "v44.25.6";

    public CheckLibrariesTask(com.tyron.builder.project.Project project, JavaModule module, ILogger logger) {
        super(project, module, logger);
    }

    @Override
    public String getName() { return "Library Guard (OMNI " + VERSION + ")"; }

    @Override
    public void prepare(BuildType type) throws IOException {
        File buildDir = getModule().getBuildDirectory().getAbsoluteFile();
        File explodedDir = new File(buildDir, "bin/res/exploded-aar").getAbsoluteFile();
        if (!explodedDir.exists()) explodedDir.mkdirs();
    }

    @Override
    public void run() throws IOException, CompilationFailedException {
        getLogger().info("🚀 OMNI: Iniciando Sincronía Sovereign " + VERSION + "...");

        if (getModule() instanceof AndroidModuleImpl) {
            ((AndroidModuleImpl) getModule()).invalidateLibraryCache();
        }

        File buildDir = getModule().getBuildDirectory().getAbsoluteFile();
        File explodedBaseDir = new File(buildDir, "bin/res/exploded-aar").getAbsoluteFile();
        
        List<File> libraries = getModule().getLibraries();
        List<File> filteredLibs = filterDuplicateKotlinLibs(libraries);

        boolean changed = false;
        for (File lib : filteredLibs) {
            if (processLibraryFast(lib, explodedBaseDir)) {
                changed = true;
            }
        }
        
        if (changed && getModule() instanceof AndroidModuleImpl) {
            ((AndroidModuleImpl) getModule()).invalidateLibraryCache();
        }
        
        try {
            verifyInfrastructureAndInstruct(explodedBaseDir);
        } catch (CompilationFailedException e) {
            // Intento de rescate final: Si el arsenal tiene las librerías pero no se explotaron bien
            if (performHeuristicRepair(filteredLibs, explodedBaseDir)) {
                 verifyInfrastructureAndInstruct(explodedBaseDir);
            } else {
                throw e;
            }
        }
        
        getLogger().info("✅ OMNI " + VERSION + ": Sincronía completada.");
    }

    private boolean performHeuristicRepair(List<File> arsenal, File baseDir) {
        getLogger().warning("⚠️ OMNI: Detectada brecha de infraestructura. Iniciando Reparación Heurística...");
        boolean repairedAny = false;
        for (File lib : arsenal) {
            if (processLibraryFast(lib, baseDir)) {
                repairedAny = true;
            }
        }
        return repairedAny;
    }

    private List<File> filterDuplicateKotlinLibs(List<File> libs) {
        Map<String, File> bestLibs = new HashMap<>();
        List<File> result = new ArrayList<>();
        
        for (File lib : libs) {
            String name = lib.getName().toLowerCase();
            if (name.contains("kotlin-stdlib")) {
                String baseName = name.contains("common") ? "kotlin-stdlib-common" : "kotlin-stdlib";
                File existing = bestLibs.get(baseName);
                if (existing == null || lib.getName().compareTo(existing.getName()) > 0) {
                    bestLibs.put(baseName, lib);
                }
            } else {
                result.add(lib);
            }
        }
        result.addAll(bestLibs.values());
        return result;
    }

    private boolean processLibraryFast(File lib, File baseDir) {
        if (lib == null || !lib.exists() || lib.length() == 0) return false;
        String name = lib.getName().toLowerCase();
        if (!name.endsWith(".aar") && !name.endsWith(".jar")) return false;

        String dirName = lib.getName().replace(".aar", "").replace(".jar", "") + "_ext";
        File destDir = new File(baseDir, dirName).getAbsoluteFile();
        
        if (destDir.exists() && destDir.lastModified() >= lib.lastModified()) {
            // Verificar integridad mínima (classes.jar debe existir)
            if (new File(destDir, "classes.jar").exists()) return false;
        }

        FileUtils.deleteQuietly(destDir);
        destDir.mkdirs();
        try {
            if (name.endsWith(".aar")) {
                Decompress.unzip(lib.getAbsolutePath(), destDir.getAbsolutePath());
            } else {
                FileUtils.copyFile(lib, new File(destDir, "classes.jar"));
            }
            destDir.setLastModified(lib.lastModified());
            return true;
        } catch (IOException e) { return false; }
    }

    private void verifyInfrastructureAndInstruct(File baseDir) throws CompilationFailedException {
        Map<String, String[]> criticals = new LinkedHashMap<>();
        criticals.put("drawerlayout", new String[]{"drawerlayout"});
        criticals.put("coordinatorlayout", new String[]{"coordinatorlayout"});
        criticals.put("recyclerview", new String[]{"recyclerview"});
        criticals.put("navigation-ui", new String[]{"navigation-ui"});
        criticals.put("arch-core", new String[]{"arch-core", "core-runtime"});
        criticals.put("tracing", new String[]{"tracing"});
        criticals.put("vectordrawable", new String[]{"vectordrawable"});
        criticals.put("vectordrawable-animated", new String[]{"vectordrawable-animated"});

        Map<String, String> downloadPaths = new HashMap<>();
        downloadPaths.put("drawerlayout", "androidx.drawerlayout/drawerlayout/1.1.1/drawerlayout-1.1.1.aar");
        downloadPaths.put("coordinatorlayout", "androidx.coordinatorlayout/coordinatorlayout/1.1.0/coordinatorlayout-1.1.0.aar");
        downloadPaths.put("recyclerview", "androidx.recyclerview/recyclerview/1.2.1/recyclerview-1.2.1.aar");
        downloadPaths.put("navigation-ui", "androidx.navigation/navigation-ui/2.7.7/navigation-ui-2.7.7.aar");
        downloadPaths.put("arch-core", "androidx.arch.core/core-runtime/2.2.0/core-runtime-2.2.0.aar");
        downloadPaths.put("tracing", "androidx.tracing/tracing/1.1.0/tracing-1.1.0.aar");
        downloadPaths.put("vectordrawable", "androidx.vectordrawable/vectordrawable/1.1.0/vectordrawable-1.1.0.aar");
        downloadPaths.put("vectordrawable-animated", "androidx.vectordrawable/vectordrawable-animated/1.1.0/vectordrawable-animated-1.1.0.aar");

        List<String> missing = new ArrayList<>();
        File[] folders = baseDir.listFiles();
        
        for (String key : criticals.keySet()) {
            boolean found = false;
            String[] aliases = criticals.get(key);
            if (folders != null) {
                for (File f : folders) {
                    String folderName = f.getName().toLowerCase();
                    for (String alias : aliases) {
                        if (folderName.contains(alias)) { found = true; break; }
                    }
                    if (found) break;
                }
            }
            if (!found) missing.add(key);
        }

        if (!missing.isEmpty()) {
            StringBuilder sb = new StringBuilder("\n🚨 OMNI: FALLO DE SUMINISTRO CRÍTICO\n");
            sb.append("Faltan eslabones esenciales para la herencia y UI:\n");
            for (String m : missing) {
                String path = downloadPaths.get(m);
                sb.append("\n📦 [").append(m).append("] Faltante.\n");
                sb.append("   👉 ACCIÓN: Copie y pegue en la Terminal OMNI:\n");
                sb.append("   curl -L \"https://maven.google.com/").append(path).append("\" -o ./app/libs/").append(m).append(".aar\n");
            }
            sb.append("\nUna vez descargados, inicie la compilación de nuevo.\n");
            throw new CompilationFailedException(sb.toString());
        }
    }
}
