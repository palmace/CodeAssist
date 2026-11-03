package com.tyron.builder.compiler.incremental.java;

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
import com.tyron.builder.model.CompileRequest;
import com.tyron.builder.project.api.JavaModule;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * IncrementalJavaTask v44.6.2: "THE ZENITH RADIANCE - SOURCE RESONANCE".
 * v44.6.2: Blindaje Nivel 13 - Validación de contexto persistente y redundancia de rutas.
 * v44.6.1: Inyección de SourcePath para resolución de símbolos entre lotes.
 */
public class IncrementalJavaTask extends Task<JavaModule> {

    private static final int MICRO_BATCH_SIZE = 20;
    private File mOutputDir;

    public IncrementalJavaTask(com.tyron.builder.project.Project project, JavaModule module, ILogger logger) {
        super(project, module, logger);
    }

    @Override public String getName() { return "Java Compiler (OMNI v44.6.2)"; }

    @Override public void prepare(BuildType type) throws IOException {
        mOutputDir = new File(getModule().getBuildDirectory().getAbsoluteFile(), "bin/java/classes");
        if (!mOutputDir.exists()) mOutputDir.mkdirs();
    }

    @Override public void run() throws IOException, CompilationFailedException {
        getLogger().info("🚀 OMNI: Iniciando Sincronía Java Zenith v44.6.2...");
        File root = getModule().getRootFile().getAbsoluteFile();
        File buildDir = getModule().getBuildDirectory().getAbsoluteFile();
        
        List<File> sourceDirs = getSourceSearchDirs(root, buildDir);
        List<File> files = collectJavaFiles(sourceDirs);
        if (files.isEmpty()) {
            getLogger().warning("⚠️ No se detectaron fuentes Java.");
            return;
        }

        Set<File> classpath = new LinkedHashSet<>();
        File baseAndroid = BuildModule.getAndroidJar();
        if (baseAndroid != null && baseAndroid.exists()) classpath.add(baseAndroid.getAbsoluteFile());
        
        List<File> projectLibs = getModule().getLibraries();
        for (File lib : projectLibs) {
            if (lib != null && lib.exists()) {
                if (lib.getName().endsWith(".aar")) {
                    File extDir = new File(buildDir, "bin/res/exploded-aar/" + lib.getName().replace(".aar", "") + "_ext");
                    File jar = new File(extDir, "classes.jar");
                    if (jar.exists()) classpath.add(jar.getAbsoluteFile());
                    else classpath.add(lib.getAbsoluteFile());
                } else {
                    classpath.add(lib.getAbsoluteFile());
                }
            }
        }
        
        File kotlinClasses = new File(buildDir, "bin/kotlin/classes");
        if (kotlinClasses.exists()) classpath.add(kotlinClasses.getAbsoluteFile());
        
        File kaptClasses = new File(buildDir, "bin/generated/kapt/classes");
        if (kaptClasses.exists()) classpath.add(kaptClasses.getAbsoluteFile());

        classpath.add(mOutputDir.getAbsoluteFile());
        
        List<String> spPaths = sourceDirs.stream().filter(File::exists).map(File::getAbsolutePath).collect(Collectors.toList());
        executeBatchedCompilation(files, classpath, spPaths);
    }

    private void executeBatchedCompilation(List<File> files, Set<File> classpath, List<String> sourcePath) throws CompilationFailedException {
        int total = files.size();
        List<String> cpPaths = classpath.stream().map(File::getAbsolutePath).collect(Collectors.toList());
        String forcedAndroidJar = BuildModule.getAndroidJar() != null ? BuildModule.getAndroidJar().getAbsolutePath() : "";

        for (int i = 0; i < total; i += MICRO_BATCH_SIZE) {
            List<File> batch = files.subList(i, Math.min(i + MICRO_BATCH_SIZE, total));
            getLogger().info("⚡ Lote Java [" + (i/MICRO_BATCH_SIZE + 1) + "/" + ((total + MICRO_BATCH_SIZE - 1)/MICRO_BATCH_SIZE) + "]");
            
            CompileRequest req = new CompileRequest();
            req.outputDir = mOutputDir.getAbsolutePath();
            req.sourceFiles = batch.stream().map(File::getAbsolutePath).collect(Collectors.toList());
            req.androidJarPath = forcedAndroidJar; 
            req.classpath = cpPaths;
            req.sourcePath = sourcePath; 
            req.allowImplicit = true; 
            delegateToService(req);
        }
    }

    private List<File> getSourceSearchDirs(File root, File buildDir) {
        List<File> dirs = new ArrayList<>();
        dirs.add(new File(root, "src/main/java"));
        dirs.add(new File(root, "app/src/main/java"));
        dirs.add(new File(buildDir, "gen"));
        dirs.add(new File(buildDir, "generated/source"));
        dirs.add(new File(buildDir, "bin/generated/kapt/sources"));
        dirs.add(new File(buildDir, "bin/generated/data_binding_base_class_source_out"));
        return dirs;
    }

    private List<File> collectJavaFiles(List<File> searchDirs) {
        List<File> files = new ArrayList<>();
        for (File d : searchDirs) {
            if (d.exists() && d.isDirectory()) {
                files.addAll(FileUtils.listFiles(d, new String[]{"java"}, true));
            }
        }
        return files.stream().distinct().collect(Collectors.toList());
    }

    private void delegateToService(CompileRequest request) throws CompilationFailedException {
        final Context context = BuildModule.getContext();
        if (context == null) throw new CompilationFailedException("Error Java Zenith: Contexto nulo.");

        final File baseCache;
        File tempCache = context.getCacheDir();
        if (tempCache == null) tempCache = context.getFilesDir();
        if (tempCache == null) throw new CompilationFailedException("Error Java Zenith: Caché inaccesible.");
        baseCache = tempCache;

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean success = new AtomicBoolean(false);
        final AtomicLong lastHeartbeat = new AtomicLong(System.currentTimeMillis());
        final StringBuilder errorLog = new StringBuilder();

        SovereignSignal.Observer observer = message -> {
            if (message == null) return;
            if (SovereignConstants.ACTION_COMPILE_RESULT.equals(message.action)) {
                success.set(message.getBooleanExtra(SovereignConstants.EXTRA_SUCCESS, false));
                String out = message.getStringExtra(SovereignConstants.EXTRA_OUTPUT);
                if (out != null) errorLog.append(out);
                latch.countDown();
            } else if (SovereignConstants.ACTION_HEARTBEAT.equals(message.action)) {
                lastHeartbeat.set(System.currentTimeMillis());
            }
        };

        File reqFile = null;
        try {
            SovereignSignal.register(observer);
            reqFile = new File(baseCache, "javac_zenith_" + System.currentTimeMillis() + ".tmp");
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(reqFile))) {
                oos.writeObject(request);
            }

            Intent intent = new Intent()
                    .setComponent(new ComponentName(context.getPackageName(), "com.tyron.code.service.JavacService"))
                    .putExtra("request_file", reqFile.getAbsolutePath());
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }

            while (!latch.await(10, TimeUnit.SECONDS)) {
                if (System.currentTimeMillis() - lastHeartbeat.get() > 60000) throw new Exception("TIMEOUT Javac Zenith");
            }
        } catch (Exception e) {
            throw new CompilationFailedException("Fallo en Túnel Java Zenith: " + e.getMessage());
        } finally {
            SovereignSignal.unregister(observer);
            if (reqFile != null) {
                final File shadowFile = reqFile;
                try { if (shadowFile.exists()) shadowFile.delete(); } catch (Throwable ignored) {}
            }
        }

        if (!success.get()) throw new CompilationFailedException("Error en enjambre Java Zenith v44.6.2: \n" + errorLog.toString());
    }
}
