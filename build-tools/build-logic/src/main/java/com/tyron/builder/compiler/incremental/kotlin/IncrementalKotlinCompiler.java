package com.tyron.builder.compiler.incremental.kotlin;

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
import com.tyron.builder.project.api.AndroidModule;
import com.tyron.builder.project.api.JavaModule;
import com.tyron.builder.project.impl.AndroidModuleImpl;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * IncrementalKotlinCompiler v2026.1800: "THE OMEGA RADIANCE - DAY 4".
 * v2026.1800: Sincronía total con el motor TITAN Nivel 1800.
 * v2026.1800: Blindaje de serialización y saneamiento de rutas.
 * v2026.1800: Erradicación forzada de amnesia estructural mediante Materia Real (Assets JAR).
 */
public class IncrementalKotlinCompiler extends Task<JavaModule> {

    private static final String VERSION = "v2026.1800";
    private File mOutputDir;

    public IncrementalKotlinCompiler(com.tyron.builder.project.Project project, JavaModule module, ILogger logger) {
        super(project, module, logger);
    }

    @Override public String getName() { return "Kotlin Compiler (OMNI " + VERSION + ")"; }

    @Override public void prepare(BuildType type) throws IOException {
        File buildDir = getModule().getBuildDirectory().getAbsoluteFile();
        mOutputDir = new File(buildDir, "bin/kotlin/classes");
        if (!mOutputDir.exists()) mOutputDir.mkdirs();
    }

    @Override public void run() throws IOException, CompilationFailedException {
        getLogger().info("🚀 OMNI: Iniciando Transmutación Kotlin Zenith " + VERSION + "...");
        
        List<File> sourceFiles = collectAllSources();
        if (sourceFiles.isEmpty() || sourceFiles.stream().noneMatch(f -> f != null && f.getName().endsWith(".kt"))) {
            getLogger().info("⏭️ No se detectaron fuentes Kotlin. Omitiendo.");
            return;
        }

        if (getModule() instanceof AndroidModuleImpl) {
             ((AndroidModuleImpl) getModule()).invalidateLibraryCache();
        }
        
        List<String> cpPaths = new ArrayList<>();
        List<String> pluginPaths = new ArrayList<>();
        for (File lib : getModule().getLibraries()) {
            if (lib == null) continue;
            String name = lib.getName().toLowerCase();
            if (name.contains("compose-compiler") || name.contains("kotlin-compose-compiler") || 
                (name.contains("compiler") && (name.contains("1.5") || name.contains("2.0")))) {
                pluginPaths.add(lib.getAbsolutePath());
            } else {
                cpPaths.add(lib.getAbsolutePath());
            }
        }

        CompileRequest req = new CompileRequest();
        req.projectRoot = getProject().getRootFile().getAbsolutePath();
        req.outputDir = (mOutputDir != null) ? mOutputDir.getAbsolutePath().trim() : "";
        req.sourceFiles = sourceFiles.stream()
                .filter(Objects::nonNull)
                .map(f -> f.getAbsolutePath().trim())
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        req.classpath = cpPaths.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
        req.sourcePath = new ArrayList<>();
        req.processorPath = new ArrayList<>();
        req.processors = new ArrayList<>();
        
        File androidJar = BuildModule.getAndroidJar();
        req.androidJarPath = (androidJar != null && androidJar.exists()) ? androidJar.getAbsolutePath().trim() : "";
        
        req.extraArgs.add("-Xskip-metadata-version-check");
        req.extraArgs.add("-api-version"); req.extraArgs.add("1.9");
        req.extraArgs.add("-language-version"); req.extraArgs.add("1.9");
        req.extraArgs.add("-Xskip-scripting-plugin");
        req.extraArgs.add("-Xdisable-default-scripting-plugin");
        
        for (String p : pluginPaths) {
            if (p != null && !p.trim().isEmpty()) {
                File pFile = new File(p.trim());
                if (pFile.exists()) {
                    req.extraArgs.add("-Xplugin=" + pFile.getAbsolutePath());
                }
            }
        }

        delegateToService(req);
        getLogger().info("✅ Transmutación Kotlin " + VERSION + " completada.");
    }

    private List<File> collectAllSources() {
        List<File> sources = new ArrayList<>();
        if (getModule() instanceof AndroidModule) {
            sources.addAll(((AndroidModule) getModule()).getKotlinFiles().values());
            File buildDir = getModule().getBuildDirectory().getAbsoluteFile();
            File genDir = new File(buildDir, "gen");
            if (genDir.exists()) {
                sources.addAll(FileUtils.listFiles(genDir, new String[]{"java"}, true));
            }
        }
        return sources.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    private void delegateToService(CompileRequest request) throws CompilationFailedException {
        final Context context = BuildModule.getContext();
        if (context == null) throw new CompilationFailedException("Error Crítico TITAN: Contexto nulo.");

        final File baseCache;
        File tempCache = context.getCacheDir();
        if (tempCache == null) tempCache = context.getFilesDir();
        if (tempCache == null) throw new CompilationFailedException("Error Crítico TITAN: Caché inaccesible.");
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
            reqFile = new File(baseCache, "kt_omega_" + System.currentTimeMillis() + ".tmp");
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(reqFile))) {
                oos.writeObject(request);
            }

            Intent intent = new Intent()
                    .setComponent(new ComponentName(context.getPackageName(), "com.tyron.code.service.KotlinService"))
                    .putExtra("request_file", reqFile.getAbsolutePath());
            
            context.startForegroundService(intent);

            while (!latch.await(10, TimeUnit.SECONDS)) {
                if (System.currentTimeMillis() - lastHeartbeat.get() > 180000) throw new Exception("TIMEOUT Crítico TITAN.");
            }
        } catch (Exception e) {
            throw new CompilationFailedException("Fallo en Túnel Kotlin Zenith " + VERSION + ": " + e.getMessage());
        } finally {
            try { SovereignSignal.unregister(observer); } catch (Throwable ignored) {}
            if (reqFile != null) {
                final File fileToDelete = reqFile;
                try { if (fileToDelete.exists()) fileToDelete.delete(); } catch (Throwable ignored) {}
            }
        }

        if (!success.get()) {
            throw new CompilationFailedException("Error Kotlin Zenith " + VERSION + ": \n" + errorLog.toString());
        }
    }
}
