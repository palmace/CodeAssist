package com.tyron.builder.compiler.apk;

import android.content.Context;

import com.tyron.builder.BuildModule;
import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.common.ApplicationProvider;
import com.tyron.common.util.BinaryExecutor;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ZipAlignTask v44.8.1: "The Forensic Aligner - Zenith Resonance".
 * v44.8.1: Quantum Cooling - Purga de memoria y estabilización pre-alineamiento.
 * v44.8.0: Sincronía con Reality Seal - Uso de rutas absolutas ancladas.
 */
public class ZipAlignTask extends Task<AndroidModule> {

    private File mApkFile;

    public ZipAlignTask(Project project, AndroidModule module, ILogger logger) {
        super(project, module, logger);
    }

    @Override
    public String getName() {
        return "ZipAlign (OMNI v44.8.1)";
    }

    @Override
    public void prepare(BuildType type) throws IOException {
        File binDir = new File(getModule().getBuildDirectory().getAbsoluteFile(), "bin");
        mApkFile = new File(binDir, "generated.apk").getAbsoluteFile();

        if (!mApkFile.exists()) {
            mApkFile = new File(getModule().getBuildDirectory().getAbsoluteFile(), "generated.apk");
        }

        if (!mApkFile.exists() || mApkFile.length() == 0) {
            throw new IOException("Error Crítico Zenith: No se encontró el APK para alinear.");
        }
        
        System.gc();
    }

    @Override
    public void run() throws IOException, CompilationFailedException {
        getLogger().info("🚀 OMNI: [Aligner] Iniciando optimización estructural Zenith v44.8.1...");
        getLogger().info("[FORENSIC] " + BuildModule.getForensicStatus());

        System.gc();
        try { Thread.sleep(300); } catch (InterruptedException ignored) {}

        File binary = getZipAlignBinary();
        File binDir = mApkFile.getParentFile();
        File outputApk = new File(binDir, "aligned.apk").getAbsoluteFile();
        
        if (outputApk.exists()) {
            FileUtils.deleteQuietly(outputApk);
        }

        List<String> args = new ArrayList<>();
        args.add(binary.getAbsolutePath());
        args.add("-v");
        args.add("-f");
        args.add("4");
        args.add(mApkFile.getAbsolutePath());
        args.add(outputApk.getAbsolutePath());
        
        BinaryExecutor executor = new BinaryExecutor();
        executor.setCommands(args);

        getLogger().info("OMNI: Ejecutando optimización binaria...");
        String logs = executor.execute();
        
        if (outputApk.exists() && outputApk.length() > 0) {
            getLogger().info("✅ OMNI v44.8.1: APK optimizado con éxito.");
            
            if (mApkFile.exists() && !mApkFile.equals(outputApk)) {
                FileUtils.deleteQuietly(mApkFile);
            }
        } else {
            getLogger().error("Error Estructural Aligner: " + logs);
            throw new CompilationFailedException("Fallo en la fase de optimización ZipAlign.");
        }
    }

    private File getZipAlignBinary() throws IOException {
        Context context = ApplicationProvider.getApplicationContext();
        if (context == null) throw new IOException("Contexto de aplicación nulo.");
        
        String path = context.getApplicationInfo().nativeLibraryDir + "/libzipalign.so";
        File file = new File(path).getAbsoluteFile();
        
        if (!file.exists()) {
            file = new File(context.getFilesDir().getParentFile(), "lib/libzipalign.so").getAbsoluteFile();
        }
        
        if (!file.exists()) throw new IOException("Binario ZipAlign no hallado.");
        return file;
    }
}
