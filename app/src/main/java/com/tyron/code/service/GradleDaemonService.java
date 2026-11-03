package com.tyron.code.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.common.base.Throwables;
import com.tyron.code.R;
import com.tyron.common.util.FileUtilsEx;

import org.apache.commons.io.FileUtils;
import org.gradle.util.GradleVersion;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * A service that runs the Gradle Daemon in a separate process.
 * Updated for Android 15 Foreground Service restrictions.
 */
public class GradleDaemonService extends Service {
    private static final String TAG = "GradleDaemonService";
    private static final String ACTION_STOP_DAEMON = "stopDaemon";
    private static final int NOTIFICATION_ID = 201;

    private boolean daemonStarted = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // En Android 15, el servicio debe iniciar en foreground casi inmediatamente
        showForegroundNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP_DAEMON.equals(intent.getAction())) {
            Log.i(TAG, "Deteniendo Gradle Daemon por solicitud del usuario.");
            stopForeground(true);
            stopSelf();
            Runtime.getRuntime().exit(0); // Forzar cierre del proceso del daemon
            return START_NOT_STICKY;
        }

        showForegroundNotification();

        if (intent == null || !intent.hasExtra("dir")) {
            return START_NOT_STICKY;
        }

        try {
            setupDaemonIO(intent.getStringExtra("dir"));
        } catch (IOException e) {
            Log.e(TAG, "Error configurando I/O del Daemon: " + e.getMessage());
        }

        if (!daemonStarted) {
            startDaemonThread();
        }

        return START_STICKY;
    }

    private void showForegroundNotification() {
        Notification notification = new NotificationCompat.Builder(this, createNotificationChannel())
                .setContentTitle("CodeAssist Gradle Daemon")
                .setSmallIcon(R.drawable.ic_stat_code)
                .setContentText("Compilando recursos y gestionando dependencias...")
                .setPriority(NotificationCompat.PRIORITY_LOW) // Menos intrusivo
                .setOngoing(true)
                .addAction(R.drawable.crash_ic_close, "DETENER", getStopPendingIntent())
                .build();

        // Fix Android 14/15: Especificar tipo de servicio
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private PendingIntent getStopPendingIntent() {
        Intent stopIntent = new Intent(this, GradleDaemonService.class);
        stopIntent.setAction(ACTION_STOP_DAEMON);
        // Fix: Usar getService en lugar de getBroadcast
        return PendingIntent.getService(
                this,
                0,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private void setupDaemonIO(String dir) throws IOException {
        File currentDir = new File(dir);
        if (!currentDir.exists()) currentDir.mkdirs();

        File daemonInput = new File(currentDir, "daemonInput");
        File daemonOutput = new File(currentDir, "daemonOutput");

        FileUtilsEx.createFile(daemonInput);
        FileUtilsEx.createFile(daemonOutput);

        // Redirección de streams para que Gradle escriba en archivos que el IDE lee
        System.setIn(FileUtils.openInputStream(daemonInput));
        System.setOut(new PrintStream(FileUtils.openOutputStream(daemonOutput, true)));
    }

    private void startDaemonThread() {
        new Thread(() -> {
            daemonStarted = true;
            Log.i(TAG, "Gradle Daemon Thread iniciado.");
            // Aquí iría la carga de Gradle vía reflexión o JNI
            // DaemonMain.main(...) 
        }, "GradleDaemonThread").start();
    }

    private String createNotificationChannel() {
        String channelId = "GradleDaemon";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannelCompat channel = new NotificationChannelCompat.Builder(channelId,
                    NotificationManagerCompat.IMPORTANCE_LOW)
                    .setName("Gradle Daemon")
                    .setDescription("Mantiene vivo el motor de compilación")
                    .build();
            NotificationManagerCompat.from(this).createNotificationChannel(channel);
        }
        return channelId;
    }
}
