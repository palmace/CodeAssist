package com.tyron.builder.compiler.apk;

import com.tyron.builder.BuildModule;
import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionMethod;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/**
 * PackageTask v44.11.5: "The Zenith Ensembler - Quantum Assembly".
 * v44.11.5: Quantum Assembly - Optimización de memoria y purga de recursos para dispositivos de recursos limitados (Redmi 13C).
 * v44.8.0: Sincronía con Reality Seal - Uso de rutas absolutas garantizadas.
 */
public class PackageTask extends Task<AndroidModule> {

    private File mApk;
    private File mResources;

    public PackageTask(Project project, AndroidModule module, ILogger logger) {
        super(project, module, logger);
    }

    @Override
    public String getName() {
        return "Package APK (OMNI v44.11.5)";
    }

    @Override
    public void prepare(BuildType type) throws IOException {
        File binDir = new File(getModule().getBuildDirectory().getAbsoluteFile(), "bin");
        mApk = new File(binDir, "generated.apk").getAbsoluteFile();
        mResources = new File(binDir, "generated.apk.res").getAbsoluteFile();
        
        if (!mResources.exists()) {
             mResources = new File(getModule().getBuildDirectory().getAbsoluteFile(), "bin/generated.apk.res");
        }
    }

    @Override
    public void run() throws IOException, CompilationFailedException {
        if (!mResources.exists() || mResources.length() == 0) {
            throw new CompilationFailedException("Error Crítico Zenith: No se encontró la materia de recursos (generated.apk.res)");
        }

        getLogger().info("🚀 OMNI: [Ensembler] Iniciando ensamble final Zenith v44.11.5...");
        getLogger().info("[FORENSIC] " + BuildModule.getForensicStatus());

        if (mApk.exists()) {
            FileUtils.deleteQuietly(mApk);
        }
        
        // OMNI v44.11.5: Purga de memoria pre-ensamblaje
        System.gc();
        
        try {
            FileUtils.copyFile(mResources, mApk);
        } catch (IOException e) {
            throw new CompilationFailedException("Fallo en clonación de recursos: " + e.getMessage());
        }

        try (ZipFile zipFile = new ZipFile(mApk)) {
            // 1. INYECCIÓN DE MATERIA BINARIA (DEX)
            File dexDir = new File(getModule().getBuildDirectory().getAbsoluteFile(), "bin/dex/out");
            File[] files = dexDir.listFiles((dir, name) -> name.endsWith(".dex"));
            
            if (files == null || files.length == 0) {
                throw new CompilationFailedException("Error de Materia: No se detectaron archivos DEX en el arsenal.");
            }

            List<File> dexFiles = new ArrayList<>(Arrays.asList(files));
            dexFiles.sort((a, b) -> a.getName().compareTo(b.getName()));
            
            getLogger().info("OMNI: [Ensembler] Inyectando " + dexFiles.size() + " fragmentos DEX...");
            
            for (File dex : dexFiles) {
                ZipParameters params = new ZipParameters();
                params.setCompressionMethod(CompressionMethod.STORE);
                params.setFileNameInZip(dex.getName());
                zipFile.addFile(dex, params);
                
                // Pausa táctica para estabilización de IO y memoria
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            }
            
            System.gc();

            // 2. INYECCIÓN DE LIBRERÍAS NATIVAS
            File nativeDir = getModule().getNativeLibrariesDirectory().getAbsoluteFile();
            if (nativeDir.exists()) {
                File[] abis = nativeDir.listFiles(File::isDirectory);
                if (abis != null) {
                    for (File abi : abis) {
                        getLogger().info("OMNI: [Ensembler] Inyectando arquitectura: " + abi.getName());
                        ZipParameters params = new ZipParameters();
                        params.setRootFolderNameInZip("lib");
                        zipFile.addFolder(abi, params);
                    }
                }
            }

            // 3. INYECCIÓN DE ASSETS
            File assetsDir = getModule().getAssetsDirectory().getAbsoluteFile();
            if (assetsDir.exists()) {
                getLogger().info("OMNI: [Ensembler] Inyectando assets...");
                ZipParameters params = new ZipParameters();
                params.setRootFolderNameInZip("assets");
                zipFile.addFolder(assetsDir, params);
            }
            
        } catch (Exception e) {
            throw new CompilationFailedException("Fallo en Ensamble Zenith v44.11.5: " + e.getMessage());
        }
        
        getLogger().info("✅ OMNI v44.11.5: APK ensamblado (" + (mApk.length() / 1024) + " KB).");
    }
}
