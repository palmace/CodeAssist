package com.tyron.builder.compiler.incremental.dex;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.tyron.builder.BuildModule;
import com.tyron.builder.SovereignConstants;
import com.tyron.builder.SovereignSignal;
import com.tyron.builder.compiler.BuildType;
import com.tyron.builder.compiler.Task;
import com.tyron.builder.exception.CompilationFailedException;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.model.DexRequest;
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.builder.project.cache.CacheHolder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * IncrementalD8Task v44.5.17: "THE ZENITH RADIANCE - ABSOLUTE RADIANCE".
 * v44.5.17: Zenith Sentry - Validación de integridad física de lotes para evitar fallos silenciosos.
 * v44.5.17: Purga de residuos térmicos y blindaje contra colisiones multidex.
 */
public class IncrementalD8Task extends Task<AndroidModule> {

    public static final CacheHolder.CacheKey<String, List<File>> CACHE_KEY = 
            new CacheHolder.CacheKey<>("dexCache");

    private static final int ZENITH_BATCH_SIZE = 25; 

    public IncrementalD8Task(com.tyron.builder.project.Project project, AndroidModule module, ILogger logger) {
        super(project, module, logger);
    }

    @Override public String getName() { return "D8 Dexer (OMNI v44.5.17)"; }

    @Override public void prepare(BuildType type) throws IOException {
        File buildDir = getModule().getBuildDirectory().getAbsoluteFile();
        File dexDir = new File(buildDir, "bin/dex");
        if (dexDir.exists()) {
            FileUtils.deleteDirectory(dexDir);
        }
        dexDir.mkdirs();
        new File(dexDir, "out").mkdirs();
        new File(dexDir, "transmuted_classes").mkdirs();
        
        // Limpieza de residuos térmicos de sesiones previas
        File[] tempDirs = dexDir.listFiles(f -> f.getName().startsWith("temp_"));
        if (tempDirs != null) {
            for (File temp : tempDirs) FileUtils.deleteQuietly(temp);
        }
    }

    @Override public void run() throws IOException, CompilationFailedException {
        getLogger().info("🚀 OMNI: Iniciando Zenith Absolute Radiance D8 (v44.5.17)...");

        List<File> projectInputs = new ArrayList<>();
        List<File> libraryInputs = new ArrayList<>();
        List<String> libraryClasspath = new ArrayList<>();
        
        File buildDir = getModule().getBuildDirectory().getAbsoluteFile();
        File transmutedDir = new File(buildDir, "bin/dex/transmuted_classes");
        
        File javaClassesDir = new File(buildDir, "bin/java/classes");
        if (javaClassesDir.exists()) projectInputs.addAll(FileUtils.listFiles(javaClassesDir, new String[]{"class"}, true));
        File kotlinOut = new File(buildDir, "bin/kotlin/classes");
        if (kotlinOut.exists()) projectInputs.addAll(FileUtils.listFiles(kotlinOut, new String[]{"class"}, true));

        List<File> allLibs = getModule().getLibraries();
        for (File lib : allLibs) {
            if (lib == null || !lib.exists()) continue;
            if (shouldDivertToClasspath(lib, libraryClasspath)) continue;
            if (isZenithCore(lib)) {
                explodeJar(lib, transmutedDir);
            } else {
                libraryInputs.add(lib);
            }
        }

        if (transmutedDir.exists()) {
            projectInputs.addAll(FileUtils.listFiles(transmutedDir, new String[]{"class"}, true));
        }

        File androidJar = BuildModule.getAndroidJar();
        if (androidJar != null) libraryClasspath.add(androidJar.getAbsolutePath());

        List<File> allProgramInputs = new ArrayList<>(projectInputs);
        allProgramInputs.addAll(libraryInputs);

        List<List<File>> batches = new ArrayList<>();
        for (int i = 0; i < allProgramInputs.size(); i += ZENITH_BATCH_SIZE) {
            batches.add(new ArrayList<>(allProgramInputs.subList(i, Math.min(i + ZENITH_BATCH_SIZE, allProgramInputs.size()))));
        }

        File finalDexDir = new File(buildDir, "bin/dex/out");
        int dexIndex = 1;
        int minApi = Math.max(24, getModule().getMinSdk());

        for (int b = 0; b < batches.size(); b++) {
            List<File> currentBatch = batches.get(b);
            int batchNum = b + 1;
            getLogger().info("⚡ Lote Zenith " + batchNum + "/" + batches.size() + " [" + currentBatch.size() + " un.]");

            DexRequest request = new DexRequest();
            request.inputs = new ArrayList<>();
            for (File f : currentBatch) request.inputs.add(f.getAbsolutePath());
            request.libraryFiles = new ArrayList<>(libraryClasspath);
            request.minApi = minApi;
            request.isRelease = false;
            
            File tempBatchDir = new File(buildDir, "bin/dex/temp_" + batchNum);
            tempBatchDir.mkdirs();
            request.outputDir = tempBatchDir.getAbsolutePath();
            
            delegateToService(request, batchNum);
            
            File[] dexFiles = tempBatchDir.listFiles((d, n) -> n.endsWith(".dex"));
            if (dexFiles != null && dexFiles.length > 0) {
                Arrays.sort(dexFiles, (f1, f2) -> f1.getName().compareTo(f2.getName()));
                for (File f : dexFiles) {
                    String target = (dexIndex == 1) ? "classes.dex" : "classes" + dexIndex + ".dex";
                    File targetFile = new File(finalDexDir, target);
                    if (targetFile.exists()) FileUtils.deleteQuietly(targetFile);
                    FileUtils.moveFile(f, targetFile);
                    dexIndex++;
                }
            } else {
                throw new CompilationFailedException("🛡️ ZENITH-SENTRY: Lote " + batchNum + " completado sin salida dex física. Transmutación fallida.");
            }
            FileUtils.deleteQuietly(tempBatchDir);
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
        getLogger().info("✅ OMNI v44.5.17: Transmutación binaria completada.");
    }

    private void explodeJar(File jarFile, File outputDir) throws IOException {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) continue;
                if (entry.getName().startsWith("META-INF/versions/")) continue;
                File outFile = new File(outputDir, entry.getName());
                if (!outFile.getParentFile().exists()) outFile.getParentFile().mkdirs();
                try (InputStream is = jar.getInputStream(entry); FileOutputStream fos = new FileOutputStream(outFile)) {
                    IOUtils.copy(is, fos);
                }
            }
        }
    }

    private boolean isZenithCore(File lib) {
        String n = lib.getName().toLowerCase();
        return n.contains("okio") || n.contains("kotlin-stdlib") || n.contains("okhttp") || n.contains("retrofit") || n.contains("gson");
    }

    private boolean shouldDivertToClasspath(File lib, List<String> classpath) {
        String name = lib.getName().toLowerCase();
        if (name.contains("stubs") || name.contains("d8") || name.contains("compiler") || name.contains("annotation")) {
            classpath.add(lib.getAbsolutePath());
            return true;
        }
        return false;
    }

    private void delegateToService(DexRequest request, int batchIndex) throws CompilationFailedException {
        final Context context = BuildModule.getContext();
        if (context == null) throw new CompilationFailedException("Error D8 Zenith: Contexto nulo.");

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean success = new AtomicBoolean(false);
        final AtomicReference<String> errorMsg = new AtomicReference<>("Fallo de Nexo");

        SovereignSignal.Observer observer = message -> {
            if (message != null && SovereignConstants.ACTION_DEX_RESULT.equals(message.action)) {
                success.set(message.getBooleanExtra(SovereignConstants.EXTRA_SUCCESS, false));
                if (!success.get()) errorMsg.set(message.getStringExtra(SovereignConstants.EXTRA_ERROR));
                latch.countDown();
            }
        };

        File reqFile = null;
        try {
            SovereignSignal.register(observer);
            File baseCache = context.getCacheDir();
            if (baseCache == null) baseCache = context.getFilesDir();
            
            reqFile = new File(baseCache, "dex_req_" + batchIndex + "_" + System.currentTimeMillis() + ".tmp");
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(reqFile))) {
                oos.writeObject(request);
            }
            Intent intent = new Intent().setComponent(new ComponentName(context.getPackageName(), "com.tyron.code.service.DexService")).putExtra("request_file", reqFile.getAbsolutePath());
            
            context.startForegroundService(intent);
            
            if (!latch.await(600, TimeUnit.SECONDS)) throw new Exception("TIMEOUT Zenith en Lote " + batchIndex);
        } catch (Exception e) {
            throw new CompilationFailedException(e.getMessage());
        } finally { 
            try { SovereignSignal.unregister(observer); } catch (Throwable ignored) {}
            if (reqFile != null) {
                final File fileToDelete = reqFile;
                try { if (fileToDelete.exists()) fileToDelete.delete(); } catch (Throwable ignored) {}
            }
        }
        if (!success.get()) throw new CompilationFailedException(errorMsg.get());
    }
}
