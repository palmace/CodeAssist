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

import com.android.tools.r8.CompilationMode;
import com.android.tools.r8.D8;
import com.android.tools.r8.D8Command;
import com.android.tools.r8.OutputMode;
import com.tyron.builder.SovereignConstants;
import com.tyron.builder.SovereignMessage;
import com.tyron.builder.SovereignSignal;
import com.tyron.builder.model.DexRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * DexService v2026.1600: "ZENITH OMEGA - TOTAL RESONANCE".
 * v2026.1600: Blindaje Nivel 1600 - Erradicación de NPE mediante saneamiento agresivo de rutas.
 * v2026.1600: Sincronía total con el protocolo de seguridad Zenith Omega para proyectos pesados.
 */
public class DexService extends Service {
    private static final String TAG = "DexService";
    private static final String VERSION = "v2026.1600";
    private static final String CHANNEL_ID = "dex_titan_channel_2026";
    private static final int NOTIFICATION_ID = 2027;

    private final ScheduledExecutorService mHeartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
    private int mHeartbeatCount = 0;

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        Notification notification = createNotification("D8 Quantum Engine: Resonancia Zenith " + VERSION + "...", 0);
        
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
            String filePath = intent.getStringExtra("request_file");
            new Thread(() -> {
                try {
                    DexRequest request = loadRequest(filePath);
                    if (request != null) {
                        mHeartbeatExecutor.scheduleWithFixedDelay(this::performPulse, 1, 5, TimeUnit.SECONDS);
                        performDexing(request);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error Dex Zenith " + VERSION + ": " + e.getMessage());
                    sendResult(false, "Fallo en la Singularidad Dex Quantum " + VERSION + ": " + e.getMessage());
                } finally {
                    mHeartbeatExecutor.shutdownNow();
                    stopForeground(true);
                    stopSelf();
                }
            }).start();
        }
        
        return START_NOT_STICKY;
    }

    private void performPulse() {
        mHeartbeatCount++;
        SovereignSignal.send(new SovereignMessage(SovereignConstants.ACTION_HEARTBEAT));
    }

    private void performDexing(DexRequest request) {
        try {
            Log.i(TAG, "🚀 DEX: Iniciando nexo Zenith " + VERSION + " de " + (request.inputs != null ? request.inputs.size() : 0) + " componentes.");
            
            List<Path> inputPaths = new ArrayList<>();
            if (request.inputs != null) {
                for (String input : request.inputs) {
                    if (input == null || input.trim().isEmpty()) continue;
                    File f = new File(input.trim()).getAbsoluteFile();
                    if (f.exists() && f.length() > 0) {
                        inputPaths.add(f.toPath());
                    }
                }
            }

            if (inputPaths.isEmpty()) {
                Log.w(TAG, "DEX: No se detectaron entradas válidas. Omitiendo.");
                sendResult(true, null);
                return;
            }

            if (request.outputDir == null || request.outputDir.trim().isEmpty()) {
                throw new Exception("Directorio de salida Dex no especificado.");
            }
            
            File outputDir = new File(request.outputDir.trim()).getAbsoluteFile();
            if (!outputDir.exists()) outputDir.mkdirs();

            D8Command.Builder builder = D8Command.builder()
                    .setMode(request.isRelease ? CompilationMode.RELEASE : CompilationMode.DEBUG)
                    .setMinApiLevel(request.minApi)
                    .setOutput(outputDir.toPath(), OutputMode.DexIndexed)
                    .setDisableDesugaring(false);

            for (Path p : inputPaths) {
                builder.addProgramFiles(p);
            }
            
            if (request.libraryFiles != null) {
                for (String lib : request.libraryFiles) {
                    if (lib == null || lib.trim().isEmpty()) continue;
                    File f = new File(lib.trim()).getAbsoluteFile();
                    if (f.exists() && f.length() > 0) {
                        builder.addLibraryFiles(f.toPath());
                    }
                }
            }

            Log.d(TAG, "DEX: Ejecutando Quantum Nexus Engine " + VERSION + "...");
            D8.run(builder.build());
            
            System.runFinalization();
            System.gc();
            
            Log.i(TAG, "✅ DEX: Transmutación Quantum Zenith " + VERSION + " completada.");
            sendResult(true, null);

        } catch (Throwable e) {
            Log.e(TAG, "Fallo crítico en el Nexo Quantum " + VERSION + ": " + e.getMessage(), e);
            sendResult(false, "Error en Nexo Zenith " + VERSION + ": " + e.getMessage() + "\n" + Log.getStackTraceString(e));
        }
    }

    private void sendResult(boolean success, String error) {
        SovereignMessage result = new SovereignMessage(SovereignConstants.ACTION_DEX_RESULT);
        result.putExtra(SovereignConstants.EXTRA_SUCCESS, success);
        if (error != null) result.putExtra(SovereignConstants.EXTRA_ERROR, error);
        SovereignSignal.send(result);
    }

    private DexRequest loadRequest(String path) {
        if (path == null || path.trim().isEmpty()) return null;
        File file = new File(path.trim()).getAbsoluteFile();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (DexRequest) ois.readObject();
        } catch (Exception e) { return null; }
        finally { 
            try {
                if (file.exists()) file.delete(); 
            } catch (Throwable ignored) {}
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "D8 Quantum Nexus", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification createNotification(String text, int progress) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("CodeAssist Zenith: Dex Quantum")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setOngoing(true)
                .setProgress(100, progress, progress == 0)
                .build();
    }
}
