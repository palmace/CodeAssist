package com.tyron.builder.compiler;

import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;

/**
 * CleanTask v44.9.0: "The Forensic Scrubber - Zenith Absolute".
 * v44.9.0: Sincronía con Reality Seal - Purga mediante rutas absolutas ancladas.
 * v44.9.0: Protocolo de Limpieza Profunda - Eliminación de sectores 'gen' y 'bin' residuales.
 * v44.9.0: Blindaje contra bloqueos de E/S en Android 15.
 */
public class CleanTask extends Task<AndroidModule> {

    public CleanTask(Project project, AndroidModule module, ILogger logger) {
        super(project, module, logger);
    }

    @Override
    public String getName() { return "Clean (OMNI v44.9.0)"; }

    @Override
    public void prepare(BuildType type) throws IOException {
        // Preparación para la purga total
    }

    @Override
    public void run() throws IOException, CompilationFailedException {
        File buildDir = getModule().getBuildDirectory().getAbsoluteFile();
        File root = getModule().getRootFile().getAbsoluteFile();
        
        getLogger().info("🚀 OMNI: Iniciando Purga Forense Zenith en " + root.getAbsolutePath());

        // 1. ELIMINACIÓN DE MATERIA RESIDUAL (build/)
        if (buildDir.exists()) {
            try {
                FileUtils.deleteDirectory(buildDir);
                getLogger().info("✅ Sector 'build' purgado con éxito.");
            } catch (IOException e) {
                getLogger().warning("⚠️ El sistema operativo bloqueó algunos archivos en 'build'. Intentando limpieza parcial...");
                try {
                    FileUtils.cleanDirectory(buildDir);
                } catch (IOException ignored) {}
            }
        }
        
        // 2. ELIMINACIÓN DE SECTOR DE GENERACIÓN (gen/)
        File genDir = new File(root, "gen").getAbsoluteFile();
        if (genDir.exists()) {
            try {
                FileUtils.deleteDirectory(genDir);
                getLogger().info("✅ Sector 'gen' purgado.");
            } catch (IOException ignored) {}
        }
        
        // 3. LIMPIEZA DE BINARIOS EN RAÍZ
        File binRoot = new File(root, "bin").getAbsoluteFile();
        if (binRoot.exists()) {
            FileUtils.deleteQuietly(binRoot);
        }

        getLogger().info("✅ OMNI v44.9.0: Caché forense purgada. Sistema listo para transmutación pura.");
    }
}
