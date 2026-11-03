package com.tyron.builder.compiler.apk;

import android.util.Log;

import com.tyron.builder.BuildModule;
import com.tyron.builder.compiler.ApkSigner;
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
 * SignTask v44.8.1: "The Forensic Signer - Zenith Absolute Seal".
 * v44.8.1: Quantum Cooling - Purga de memoria pre-sellado para asegurar estabilidad en dispositivos de 4GB.
 * v44.8.0: Sincronía con Reality Seal - Garantiza el sellado final post-ensamble determinista.
 */
public class SignTask extends Task<AndroidModule> {

    private File mInputApk;
    private File mOutputApk;

    public SignTask(Project project, AndroidModule module, ILogger logger) {
        super(project, module, logger);
    }

    @Override
    public String getName() {
        return "Sign APK (OMNI v44.8.1)";
    }

    @Override
    public void prepare(BuildType type) throws IOException {
        File binDir = new File(getModule().getBuildDirectory().getAbsoluteFile(), "bin");
        
        mInputApk = new File(binDir, "aligned.apk").getAbsoluteFile();
        if (!mInputApk.exists()) {
            mInputApk = new File(binDir, "generated.apk").getAbsoluteFile();
        }

        mOutputApk = new File(binDir, "signed.apk").getAbsoluteFile();

        if (!mInputApk.exists() || mInputApk.length() == 0) {
            throw new IOException("Error Crítico Zenith: No se encontró la materia APK para sellar.");
        }
    }

    @Override
    public void run() throws IOException, CompilationFailedException {
        getLogger().info("🚀 OMNI: [Signer] Sellando APK con Protocolo Zenith v44.8.1...");
        getLogger().info("[FORENSIC] " + BuildModule.getForensicStatus());

        // OMNI v44.8.1: Purga de memoria pre-sellado
        System.gc();
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        ApkSigner signer = new ApkSigner(mInputApk.getAbsolutePath(),
                mOutputApk.getAbsolutePath(), ApkSigner.Mode.TEST);

        try {
            signer.sign();
            
            if (!mOutputApk.exists() || mOutputApk.length() == 0) {
                throw new Exception("El APK sellado no se materializó correctamente.");
            }
            
            getLogger().info("✅ OMNI v44.8.1: APK sellado con éxito (" + (mOutputApk.length() / 1024) + " KB).");
        } catch (Exception e) {
            throw new CompilationFailedException("Fallo en sellado Zenith v44.8.1: " + e.getMessage());
        }

        // Purga de materia intermedia
        if (mInputApk.exists() && !mInputApk.equals(mOutputApk)) {
            FileUtils.deleteQuietly(mInputApk);
        }
    }
}
