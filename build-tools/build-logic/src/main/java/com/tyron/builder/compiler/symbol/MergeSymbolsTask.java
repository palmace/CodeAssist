package com.tyron.builder.compiler.symbol;

import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.builder.compiler.resource.AAPT2Compiler;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * SymbolProcessor v44.8.1: "The Neural Bridge - Full Spectrum".
 * v44.8.1: Blindaje forzado de eslabones críticos (interpolator, navigation, etc.) para evitar ENOENT.
 * v44.8.0: Sincronía Total con Reality Seal - Uso de rutas absolutas ancladas.
 */
public class MergeSymbolsTask extends Task<AndroidModule> {

    public MergeSymbolsTask(Project project, AndroidModule module, ILogger logger) {
        super(project, module, logger);
    }

    @Override
    public String getName() { return "SymbolProcessor (OMNI v44.8.1)"; }

    @Override
    public void prepare(BuildType type) throws IOException {
        File genDir = new File(getModule().getBuildDirectory().getAbsoluteFile(), "gen").getAbsoluteFile();
        // OMNI: Limpieza suave para no colisionar con procesos en paralelo
        if (genDir.exists()) {
            try { FileUtils.cleanDirectory(genDir); } catch (IOException e) { genDir.delete(); genDir.mkdirs(); }
        } else {
            genDir.mkdirs();
        }
    }

    @Override
    public void run() throws IOException, CompilationFailedException {
        File buildDir = getModule().getBuildDirectory().getAbsoluteFile();
        getLogger().info("🚀 OMNI: Iniciando Sincronía Estructural Neural v44.8.1...");

        File rTxt = findRTxt(buildDir);
        if (rTxt == null || !rTxt.exists()) {
            throw new CompilationFailedException("Error Crítico: R.txt no hallado. AAPT2 Link falló o la ruta se evaporó.");
        }

        SymbolLoader loader = new SymbolLoader(rTxt, getLogger());
        try { 
            loader.load(); 
        } catch (IOException e) { 
            throw new CompilationFailedException("Fallo cargando símbolos: " + e.getMessage()); 
        }

        Set<String> packages = collectPackagesDynamically();
        getLogger().info("Neural Bridge: Sincronizando " + packages.size() + " constelaciones de recursos.");

        File genDir = new File(buildDir, "gen").getAbsoluteFile();
        for (String pkg : packages) {
            if (pkg == null || pkg.isEmpty()) continue;
            
            File packageDir = new File(genDir, pkg.replace('.', '/')).getAbsoluteFile();
            if (!packageDir.exists() && !packageDir.mkdirs()) {
                // Si mkdirs falla, intentamos una vez más con un pequeño delay (latencia de IO en Android)
                try { Thread.sleep(10); } catch (InterruptedException ignored) {}
                packageDir.mkdirs();
            }
            
            SymbolWriter writer = new SymbolWriter(genDir.getAbsolutePath(), pkg, loader, getModule());
            writer.addSymbolsToWrite(loader);
            writer.write();
        }
        
        getLogger().info("✅ OMNI v44.8.1: Sincronización estructural completada.");
    }

    private Set<String> collectPackagesDynamically() {
        Set<String> packages = new LinkedHashSet<>();
        
        String mainPkg = getModule().getPackageName();
        if (mainPkg != null && !mainPkg.isEmpty()) {
            packages.add(mainPkg);
            packages.add(mainPkg + ".databinding");
        }
        
        File buildDir = getModule().getBuildDirectory().getAbsoluteFile();
        File explodedDir = new File(buildDir, "bin/res/exploded-aar").getAbsoluteFile();
        
        if (explodedDir.exists() && explodedDir.isDirectory()) {
            File[] aarDirs = explodedDir.listFiles(File::isDirectory);
            if (aarDirs != null) {
                for (File aarDir : aarDirs) {
                    File manifest = new File(aarDir, "AndroidManifest.xml").getAbsoluteFile();
                    if (manifest.exists()) {
                        try {
                            String pkg = AAPT2Compiler.getPackageName(manifest);
                            if (pkg != null && !pkg.isEmpty()) {
                                packages.add(pkg);
                            }
                        } catch (Exception e) {
                            getLogger().warning("⚠️ No se pudo extraer paquete de: " + aarDir.getName());
                        }
                    }
                }
            }
        }

        // Blindaje de Emergencia para Infraestructura Crítica (Zenith Spectrum)
        String[] criticalPaths = {
            "com.google.android.material",
            "androidx.appcompat",
            "androidx.drawerlayout",
            "androidx.coordinatorlayout",
            "androidx.recyclerview",
            "androidx.constraintlayout",
            "androidx.cardview",
            "androidx.core",
            "androidx.fragment",
            "androidx.activity",
            "androidx.lifecycle",
            "androidx.vectordrawable",
            "androidx.vectordrawable.animated",
            "androidx.interpolator",
            "androidx.cursoradapter",
            "androidx.navigation.ui",
            "androidx.navigation.common",
            "androidx.navigation.runtime",
            "androidx.navigation.fragment",
            "androidx.viewpager",
            "androidx.viewpager2",
            "androidx.customview"
        };
        
        for (String cp : criticalPaths) {
            packages.add(cp);
        }
        
        return packages;
    }

    private File findRTxt(File buildDir) {
        File[] paths = { 
            new File(buildDir, "bin/res/R.txt").getAbsoluteFile(), 
            new File(buildDir, "bin/R.txt").getAbsoluteFile(), 
            new File(buildDir, "R.txt").getAbsoluteFile(),
            new File(getModule().getRootFile(), "build/bin/res/R.txt").getAbsoluteFile()
        };
        for (File p : paths) {
            if (p.exists() && p.length() > 0) return p;
        }
        return null;
    }
}
