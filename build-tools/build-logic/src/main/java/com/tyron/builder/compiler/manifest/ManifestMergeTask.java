package com.tyron.builder.compiler.manifest;

import com.tyron.builder.BuildModule;
import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;

/**
 * Gestor de Manifiesto v41.5.0: "The Immortal Zenith Merger - Reality Sync".
 * v41.5.0: Sincronía con Reality Seal - Delegación de búsqueda al Radar del Módulo.
 * v41.5.0: Blindaje contra 'Manifiesto ausente' mediante anclaje absoluto.
 */
public class ManifestMergeTask extends Task<AndroidModule> {

    private File mOutputFile;
    private File mMainManifest;

    public ManifestMergeTask(Project project, AndroidModule module, ILogger logger) {
        super(project, module, logger);
    }

    @Override
    public String getName() { return "ManifestMerger (OMNI v41.5.0)"; }

    @Override
    public void prepare(BuildType type) throws IOException {
        AndroidModule module = getModule();
        
        // v41.5.0: Utilizamos el Radar de Supervivencia v4 del módulo para asegurar el anclaje
        mMainManifest = module.getManifestFile().getAbsoluteFile();
        
        if (!mMainManifest.exists()) {
             // Fallback estructural manual por si el modelo aún no ha indexado
             mMainManifest = findManifestManual(module.getRootFile().getAbsoluteFile());
        }

        if (mMainManifest == null || !mMainManifest.exists()) {
            throw new IOException("Fallo Crítico Zenith: El radar no localiza AndroidManifest.xml en " + module.getRootFile().getAbsolutePath());
        }

        File buildDir = module.getBuildDirectory().getAbsoluteFile();
        File binDir = new File(buildDir, "bin").getAbsoluteFile();
        
        try {
            if (!binDir.exists()) FileUtils.forceMkdir(binDir);
        } catch (IOException e) {
            binDir = new File(BuildModule.getContext().getCacheDir(), "omni_bin").getAbsoluteFile();
            if (!binDir.exists()) binDir.mkdirs();
        }
        
        mOutputFile = new File(binDir, "AndroidManifest.xml").getAbsoluteFile();
    }

    private File findManifestManual(File root) {
        File[] candidates = {
            new File(root, "src/main/AndroidManifest.xml"),
            new File(root, "app/src/main/AndroidManifest.xml"),
            new File(root, "AndroidManifest.xml"),
            new File(root.getParentFile(), "src/main/AndroidManifest.xml")
        };
        for (File f : candidates) {
            if (f != null && f.exists() && f.length() > 0) return f.getAbsoluteFile();
        }
        return null;
    }

    @Override
    public void run() throws IOException, CompilationFailedException {
        if (mMainManifest != null && mMainManifest.exists()) {
            getLogger().info("OMNI: [Reality Sync] Anclando manifiesto: " + mMainManifest.getAbsolutePath());
            FileUtils.copyFile(mMainManifest, mOutputFile);
            
            // Verificación de integridad post-copia
            if (!mOutputFile.exists() || mOutputFile.length() == 0) {
                throw new CompilationFailedException("Error de Transmutación: El manifiesto copiado está vacío o es inaccesible.");
            }
        } else {
            throw new CompilationFailedException("Error Crítico Zenith: El manifiesto ha desaparecido durante la fase de anclaje.");
        }
    }
}
