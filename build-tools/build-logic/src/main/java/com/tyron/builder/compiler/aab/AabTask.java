package com.tyron.builder.compiler.aab;

import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.project.Project;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.common.util.Decompress;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import com.tyron.builder.compiler.BundleTool;

/**
 * AabTask v44.11.0: "The Zenith Bundle Master - Structural Reality".
 * v44.11.0: Sincronía con Reality Seal - Uso de rutas absolutas para evitar colapsos en Android 15.
 * v44.11.0: Protocolo de Ensamblaje Estructural - Organización precisa de sectores base/manifest, base/dex, base/res.
 * v44.11.0: Rescate de materia nativa y recursos de infraestructura (Material Design).
 */
public class AabTask extends Task<AndroidModule> {

    private static final String TAG = "AabTask";

    private File mBinDir;
    private File mBaseDir;
    private File mManifestTarget;
    private File mDexTarget;
    private File mLibTarget;
    private File mAssetsTarget;
    
    private File mModuleZip;
    private File mOutputAab;

    public AabTask(Project project, AndroidModule module, ILogger logger) {
        super(project, module, logger);
    }

    @Override
    public String getName() {
        return "Package AAB (OMNI v44.11.0)";
    }

    @Override
    public void prepare(BuildType type) throws IOException {
        mBinDir = new File(getModule().getBuildDirectory().getAbsoluteFile(), "bin");
        mBaseDir = new File(mBinDir, "base").getAbsoluteFile();

        // v44.11.0: Purga y reconstrucción de la zona de aterrizaje
        if (mBaseDir.exists()) FileUtils.deleteDirectory(mBaseDir);
        mBaseDir.mkdirs();

        mManifestTarget = new File(mBaseDir, "manifest");
        mManifestTarget.mkdirs();
        
        mDexTarget = new File(mBaseDir, "dex");
        mDexTarget.mkdirs();
        
        mLibTarget = new File(mBaseDir, "lib");
        mLibTarget.mkdirs();
        
        mAssetsTarget = new File(mBaseDir, "assets");
        mAssetsTarget.mkdirs();

        mModuleZip = new File(mBinDir, "Base-Module.zip").getAbsoluteFile();
        mOutputAab = new File(mBinDir, "module.aab").getAbsoluteFile();
        
        if (mModuleZip.exists()) mModuleZip.delete();
        if (mOutputAab.exists()) mOutputAab.delete();
    }

    @Override
    public void run() throws IOException, CompilationFailedException {
        getLogger().info("🚀 OMNI: [BundleMaster] Iniciando transmutación de distribución Zenith v44.11.0...");
        
        try {
            // 1. DESCOMPRESIÓN DE PROTO-FORMAT (Generado por AAPT2 Zenith)
            extractProtoFormat();
            
            // 2. REORGANIZACIÓN ESTRUCTURAL
            organizeManifest();
            collectDexFiles();
            collectNativeLibs();
            collectAssets();
            collectJavaResources();
            
            // 3. CREACIÓN DE MATERIA MODULE (Base-Module.zip)
            createModuleZip();
            
            // 4. INYECCIÓN DE LIBRERÍAS EXTERNAS (Repo Zenith)
            injectExternalLibraries();
            
            // 5. TRANSMUTACIÓN FINAL A AAB
            generateBundle();
            
            getLogger().info("✅ OMNI v44.11.0: Bundle transmutado con éxito: " + mOutputAab.getAbsolutePath());
            
        } catch (Exception e) {
            throw new CompilationFailedException("Fallo en la distribución Zenith: " + e.getMessage());
        }
    }

    private void extractProtoFormat() throws IOException {
        File protoZip = new File(mBinDir, "proto-format.zip").getAbsoluteFile();
        if (!protoZip.exists()) {
            throw new IOException("Materia Proto-Format no hallada. ¿Falló el enlace de AAPT2?");
        }
        getLogger().info("OMNI: [BundleMaster] Extrayendo base Proto-Format...");
        Decompress.unzip(protoZip.getAbsolutePath(), mBaseDir.getAbsolutePath());
    }

    private void organizeManifest() throws IOException {
        File rootManifest = new File(mBaseDir, "AndroidManifest.xml");
        if (rootManifest.exists()) {
            FileUtils.moveFile(rootManifest, new File(mManifestTarget, "AndroidManifest.xml"));
        } else {
            // Fallback al manifiesto original si AAPT2 no lo incluyó en el zip
            File original = getModule().getManifestFile().getAbsoluteFile();
            if (original.exists()) {
                FileUtils.copyFile(original, new File(mManifestTarget, "AndroidManifest.xml"));
            }
        }
    }

    private void collectDexFiles() throws IOException {
        getLogger().info("OMNI: [BundleMaster] Recolectando fragmentos DEX...");
        File dexSourceDir = new File(mBinDir, "dex/out").getAbsoluteFile();
        File[] dexFiles = dexSourceDir.listFiles((dir, name) -> name.endsWith(".dex"));
        if (dexFiles != null) {
            for (File dex : dexFiles) {
                FileUtils.copyFileToDirectory(dex, mDexTarget);
            }
        }
    }

    private void collectNativeLibs() throws IOException {
        File nativeSource = getModule().getNativeLibrariesDirectory().getAbsoluteFile();
        if (nativeSource.exists()) {
            getLogger().info("OMNI: [BundleMaster] Recolectando artillería nativa...");
            copyDirectoryRecursive(nativeSource.toPath(), mLibTarget.toPath());
        }
    }

    private void collectAssets() throws IOException {
        File assetsSource = getModule().getAssetsDirectory().getAbsoluteFile();
        if (assetsSource.exists()) {
            copyDirectoryRecursive(assetsSource.toPath(), mAssetsTarget.toPath());
        }
    }

    private void collectJavaResources() throws IOException {
        File resDir = getModule().getResourcesDir().getAbsoluteFile();
        if (resDir.exists()) {
            File rootTarget = new File(mBaseDir, "root");
            rootTarget.mkdirs();
            copyDirectoryRecursive(resDir.toPath(), rootTarget.toPath());
        }
    }

    private void createModuleZip() throws IOException {
        getLogger().info("OMNI: [BundleMaster] Sellando materia del módulo base...");
        zipFolder(mBaseDir.toPath(), mModuleZip.toPath());
    }

    private void injectExternalLibraries() throws IOException {
        List<File> libraries = getModule().getLibraries();
        try (ZipFile zipFile = new ZipFile(mModuleZip)) {
            for (File lib : libraries) {
                if (lib == null || !lib.exists() || !lib.getName().endsWith(".jar")) continue;
                
                try (JarFile jar = new JarFile(lib)) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.isDirectory() || entry.getName().startsWith("META-INF") || entry.getName().endsWith(".class")) {
                            continue;
                        }
                        
                        String zipPath = "root/" + entry.getName();
                        ZipParameters params = new ZipParameters();
                        params.setFileNameInZip(zipPath);
                        params.setCompressionLevel(CompressionLevel.FASTEST);
                        
                        try (InputStream is = jar.getInputStream(entry)) {
                            zipFile.addStream(is, params);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private void generateBundle() throws Exception {
        getLogger().info("OMNI: [BundleMaster] Ejecutando Transmutación BundleTool...");
        BundleTool tool = new BundleTool(mModuleZip.getAbsolutePath(), mOutputAab.getAbsolutePath());
        tool.aab();
    }

    private void zipFolder(final Path source, Path target) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(target.toFile()))) {
            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    zos.putNextEntry(new ZipEntry(source.relativize(file).toString()));
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private void copyDirectoryRecursive(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new TreeCopyFileVisitor(source.toString(), target.toString()));
    }

    @Override
    protected void clean() {
        if (mBaseDir != null && mBaseDir.exists()) {
            FileUtils.deleteQuietly(mBaseDir);
        }
    }
}
