package com.tyron.builder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;
import java.io.File;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * BuildModule v2026.42: "The Sovereign Shield".
 * v2026.42: Blindaje absoluto contra SecurityException en Android 14/HyperOS.
 * v2026.42: Uso de flags explícitos en todos los registros de receiver.
 */
public class BuildModule {

    private static Context sApplicationContext;
    private static File sAndroidJar;
    private static File sLambdaStubs;

    public static void initialize(Context applicationContext) {
        sApplicationContext = applicationContext.getApplicationContext();
    }

    public static Context getContext() {
        return sApplicationContext;
    }

    /**
     * Obtiene temperatura y hora para el log forense.
     * v2026.42: Reparado para cumplir con políticas de registro de Android 14.
     */
    public static String getForensicStatus() {
        String temp = "N/A";
        try {
            Context ctx = getContext();
            if (ctx != null) {
                IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent intent;
                // OMNI v2026.42: En Android 14+, incluso registros con receiver nulo requieren flags si no son para broadcasts del sistema.
                // Aunque BATTERY_CHANGED es del sistema, HyperOS es estricto.
                if (Build.VERSION.SDK_INT >= 33) {
                    intent = ctx.registerReceiver(null, filter, 4); // 4 = RECEIVER_NOT_EXPORTED
                } else {
                    intent = ctx.registerReceiver(null, filter);
                }
                
                if (intent != null) {
                    int t = intent.getIntExtra("temperature", 0);
                    temp = (t / 10.0f) + "°C";
                }
            } else {
                temp = "Host-PC";
            }
        } catch (Exception ignored) {}
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        return "Temp: " + temp + " | Hora: " + time;
    }

    /**
     * Registro Seguro v21.0: Blindaje total mediante reflexión profunda y desanidamiento de contexto.
     * flags: 2 = RECEIVER_EXPORTED, 4 = RECEIVER_NOT_EXPORTED.
     */
    public static void registerReceiverSecure(Context context, BroadcastReceiver receiver, IntentFilter filter, int flags) {
        if (context == null || receiver == null || filter == null) return;
        try {
            Context base = context;
            // Desanidamos el contexto hasta llegar al ContextImpl nativo para saltar interceptores de Xiaomi/HyperOS
            while (base instanceof ContextWrapper && ((ContextWrapper) base).getBaseContext() != null) {
                Context next = ((ContextWrapper) base).getBaseContext();
                if (next == base) break;
                base = next;
            }
            
            try { base.unregisterReceiver(receiver); } catch (Throwable ignored) {}

            if (Build.VERSION.SDK_INT >= 33) {
                int safeFlags = (flags == 0) ? 4 : flags;
                try {
                    // Intento 1: Reflexión sobre el método con flags
                    Method m = Context.class.getMethod("registerReceiver", 
                        BroadcastReceiver.class, IntentFilter.class, int.class);
                    m.invoke(base, receiver, filter, safeFlags);
                } catch (Throwable t) {
                    // Intento 2: Llamada nativa directa si la reflexión falló (poco probable en API 33+)
                    base.registerReceiver(receiver, filter, safeFlags);
                }
            } else {
                // Legacy: Android 12 y anteriores
                base.registerReceiver(receiver, filter);
            }
        } catch (Exception e) {
            Log.e("OMNI_LOG", "ERROR REGISTRO CRÍTICO: " + e.getMessage());
        }
    }

    public static File getAndroidJar() { return sAndroidJar; }
    public static void setAndroidJar(File jar) { sAndroidJar = jar; }
    public static File getLambdaStubs() { return sLambdaStubs; }
    public static void setLambdaStubs(File file) { sLambdaStubs = file; }
}
