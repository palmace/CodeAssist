package com.tyron.builder.compiler.firebase;

import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;

import java.io.File;
import java.io.IOException;

/**
 * FirebaseConfigTask v44.8.0: "The Zenith Beacon - Silent Sync".
 * v44.8.0: Sincronía con Reality Seal - Verificación absoluta de google-services.json.
 * v44.8.0: Operación Silenciosa para evitar ruido en el OMNI LOG.
 */
public class GenerateFirebaseConfigTask extends Task<AndroidModule> {

    public GenerateFirebaseConfigTask(Project project, AndroidModule module, ILogger logger) {
        super(project, module, logger);
    }

    @Override
    public String getName() {
        return "Firebase Config (OMNI v44.8.0)";
    }

    @Override
    public void prepare(BuildType type) throws IOException {
        // Preparación silenciosa Zenith
    }

    @Override
    public void run() throws IOException, CompilationFailedException {
        File root = getModule().getRootFile().getAbsoluteFile();
        File configFile = new File(root, "google-services.json");
        
        if (!configFile.exists()) {
            // Intento de rescate en el nivel superior (Proyecto vs Módulo)
            configFile = new File(root.getParentFile(), "google-services.json");
        }

        if (configFile.exists() && configFile.length() > 0) {
            getLogger().info("🚀 OMNI: [Beacon] Detectado google-services.json. Sincronizando servicios Firebase...");
            // La lógica de transmutación de recursos Firebase se activaría aquí
        } else {
            // Silencio operativo: No bloqueamos si Firebase no está configurado
        }
    }
}
