package com.tyron.code.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.tyron.builder.SovereignConstants;
import com.tyron.builder.SovereignMessage;
import com.tyron.builder.SovereignSignal;
import com.tyron.builder.model.CompileRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

/**
 * JavacService v2026.1600: "ZENITH OMEGA - TOTAL RESONANCE".
 * v2026.1600: Sincronía total con el núcleo TITAN Nivel 1600.
 * v2026.1600: Blindaje de rutas y saneamiento de classpath para proyectos pesados.
 */
public class JavacService extends Service {
    
    private static final String TAG = "JavacService";
    private static final String VERSION = "v2026.1600";
    private static final String CHANNEL_ID = "compilation_channel_2026";
    private static final int NOTIFICATION_ID = 2025;
    
    private JavacTool mTool;

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        mTool = JavacTool.create();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        Notification notification = createNotification("TITAN Java: Resonancia Zenith " + VERSION + "...");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } catch (Exception e) {
                startForeground(NOTIFICATION_ID, notification);
            }
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        if (intent.hasExtra("request_file")) {
            handleCompileRequest(intent, startId);
        }

        return START_NOT_STICKY;
    }

    private void handleCompileRequest(Intent intent, int startId) {
        String reqPath = intent.getStringExtra("request_file");
        new Thread(() -> {
            ScheduledExecutorService batchHeartbeat = Executors.newSingleThreadScheduledExecutor();
            try {
                CompileRequest request = loadRequest(reqPath);
                if (request != null) {
                    batchHeartbeat.scheduleWithFixedDelay(() -> 
                        SovereignSignal.send(new SovereignMessage(SovereignConstants.ACTION_HEARTBEAT)), 1, 5, TimeUnit.SECONDS);
                    performCompilation(request);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error Build Crítico " + VERSION + ": " + e.getMessage());
                sendResult(false, "Excepción en el núcleo Zenith " + VERSION + ": " + e.getMessage());
            } finally {
                batchHeartbeat.shutdownNow();
                stopSelf(startId);
            }
        }).start();
    }

    private void performCompilation(CompileRequest request) {
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        StandardJavaFileManager fm = mTool.getStandardFileManager(collector, null, null);
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);
        
        try {
            List<File> sourceFiles = new ArrayList<>();
            if (request.sourceFiles != null) {
                for (String s : request.sourceFiles) {
                    if (s == null || s.trim().isEmpty()) continue;
                    File f = new File(s.trim());
                    if (f.exists()) sourceFiles.add(f);
                }
            }
            
            if (sourceFiles.isEmpty()) {
                sendResult(true, "No hay archivos fuente Java para compilar.");
                return;
            }

            if (request.outputDir != null && !request.outputDir.trim().isEmpty()) {
                File outputDir = new File(request.outputDir.trim());
                if (!outputDir.exists()) outputDir.mkdirs();
                fm.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(outputDir));
            }
            
            String absoluteAndroidJar = request.androidJarPath;
            if (absoluteAndroidJar != null && !absoluteAndroidJar.trim().isEmpty()) {
                File sdkJar = new File(absoluteAndroidJar.trim());
                if (sdkJar.exists()) {
                    fm.setLocation(StandardLocation.PLATFORM_CLASS_PATH, Collections.singletonList(sdkJar));
                }
            }

            if (request.sourcePath != null && !request.sourcePath.isEmpty()) {
                List<File> sourcePath = new ArrayList<>();
                for (String sp : request.sourcePath) {
                    if (sp == null || sp.trim().isEmpty()) continue;
                    File f = new File(sp.trim());
                    if (f.exists()) sourcePath.add(f);
                }
                if (!sourcePath.isEmpty()) {
                    fm.setLocation(StandardLocation.SOURCE_PATH, sourcePath);
                }
            }

            List<File> classpath = new ArrayList<>();
            if (request.classpath != null) {
                for (String cp : request.classpath) {
                    if (cp == null || cp.trim().isEmpty()) continue;
                    File f = new File(cp.trim());
                    if (f.exists()) classpath.add(f);
                }
            }
            fm.setLocation(StandardLocation.CLASS_PATH, classpath);

            Iterable<? extends JavaFileObject> units = fm.getJavaFileObjectsFromFiles(sourceFiles);
            
            List<String> options = new ArrayList<>(Arrays.asList(
                "-g", 
                "-proc:none", 
                "-Xlint:none",
                "-source", "8",
                "-target", "8",
                "-encoding", "UTF-8",
                "-XDignore.symbol.file"
            ));
            
            if (request.allowImplicit) {
                options.add("-implicit:class");
            }

            JavacTask task = (JavacTask) mTool.getTask(writer, fm, collector, options, null, units);
            boolean success = task.call();

            StringBuilder sb = new StringBuilder();
            for (Diagnostic<? extends JavaFileObject> diagnostic : collector.getDiagnostics()) {
                sb.append(diagnostic.toString()).append("\n");
            }
            sb.append(out.toString());

            sendResult(success, sb.toString());
            
        } catch (Exception e) {
            Log.e(TAG, "Fallo Compilación Zenith " + VERSION + ": " + e.getMessage());
            sendResult(false, "Fallo interno en Transmutación Java " + VERSION + ": " + e.getMessage());
        } finally {
            try { fm.close(); } catch (Exception ignored) {}
        }
    }

    private void sendResult(boolean success, String output) {
        SovereignMessage result = new SovereignMessage(SovereignConstants.ACTION_COMPILE_RESULT);
        result.putExtra(SovereignConstants.EXTRA_SUCCESS, success);
        result.putExtra(SovereignConstants.EXTRA_OUTPUT, output);
        SovereignSignal.send(result);
    }

    private CompileRequest loadRequest(String path) {
        if (path == null) return null;
        File file = new File(path);
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (CompileRequest) ois.readObject();
        } catch (Exception e) { return null; }
        finally { 
            try {
                if (file.exists()) file.delete(); 
            } catch (Throwable ignored) {}
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Java TITAN", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("CodeAssist Zenith: Java Engine")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setOngoing(true)
                .build();
    }
}
