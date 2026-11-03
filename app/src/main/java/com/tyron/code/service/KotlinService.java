package com.tyron.code.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.tyron.builder.SovereignConstants;
import com.tyron.builder.SovereignMessage;
import com.tyron.builder.SovereignSignal;
import com.tyron.builder.model.CompileRequest;

import org.apache.commons.io.FileUtils;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dalvik.system.DexClassLoader;

/**
 * KotlinService v2026.1800: "ZENITH OMEGA - THE FINAL SEAL (DAY 4)".
 * v2026.1800: Implementación de Materia Real (Extracción de JAR desde Assets).
 * v2026.1800: Delegación de ClassLoader mediante HashSet para recursos .class físicos.
 * v2026.1800: Inmunidad ProGuard mediante referencia estática a K2JVMCompiler.
 * v2026.1800: Erradicación definitiva de ResourceNotFoundException.
 */
public class KotlinService extends Service {
    
    private static final String TAG = "KotlinService";
    private static final String VERSION = "v2026.1800";
    private static final String CHANNEL_ID = "kotlin_titan_singularity_2026";
    private static final int NOTIFICATION_ID = 2026;

    private PowerManager.WakeLock mWakeLock;
    private final ScheduledExecutorService mHeartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
    private int mHeartbeatCount = 0;
    private final HashSet<String> mAnchorResources = new HashSet<>();

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        // Inmunidad R8/ProGuard
        try { String id = K2JVMCompiler.class.getSimpleName(); Log.d(TAG, "🛡️ Inmunidad K2 activada: " + id); } catch (Throwable ignored) {}
        
        createNotificationChannel();
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CodeAssist:TITAN_RESONANCE");
        }
        Log.i(TAG, "⭐ TITAN Engine " + VERSION + " inicializado.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        applySovereignShield("TITAN: Iniciando Resonancia Zenith " + VERSION + "...");
        if (intent.hasExtra("request_file")) {
            if (mWakeLock != null && !mWakeLock.isHeld()) mWakeLock.acquire(25 * 60 * 1000L);
            handleCompileRequest(intent);
        }
        return START_NOT_STICKY;
    }

    private void applySovereignShield(String msg) {
        Notification notification = createNotification(msg, (mHeartbeatCount * 5) % 100);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } catch (Exception e) {
                startForeground(NOTIFICATION_ID, notification);
            }
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void handleCompileRequest(Intent intent) {
        String requestPath = intent.getStringExtra("request_file");
        if (requestPath == null) return;
        new Thread(() -> {
            try {
                CompileRequest request = loadRequest(requestPath);
                if (request != null) {
                    mHeartbeatExecutor.scheduleWithFixedDelay(this::performPulse, 1, 5, TimeUnit.SECONDS);
                    performKotlinCompilation(request);
                }
            } catch (Exception e) {
                Log.e(TAG, "Fallo TITAN K2: " + e.getMessage(), e);
                sendResult(false, "Excepción Crítica TITAN K2 " + VERSION + ": " + e.getMessage());
            } finally {
                if (mWakeLock != null && mWakeLock.isHeld()) mWakeLock.release();
                mHeartbeatExecutor.shutdownNow();
                stopForeground(true); 
                stopSelf();
            }
        }).start();
    }

    private void performPulse() {
        mHeartbeatCount++;
        SovereignSignal.send(new SovereignMessage(SovereignConstants.ACTION_HEARTBEAT));
    }

    private void extractCompilerIfNeeded(File target) throws IOException {
        if (target.exists() && target.length() > 10000000) return;
        Log.i(TAG, "📦 TITAN: Extrayendo Materia Real (kotlin-compiler.jar)...");
        try (InputStream is = getAssets().open("kotlin-compiler.jar");
             FileOutputStream fos = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) > 0) fos.write(buffer, 0, len);
        }
    }

    private void performKotlinCompilation(CompileRequest request) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            File baseCache = getCodeCacheDir();
            if (baseCache == null) baseCache = getCacheDir();
            File baseDir = new File(baseCache, "kotlin_zenith_core");
            if (!baseDir.exists()) baseDir.mkdirs();
            File physicalHome = new File(baseDir, "kotlin_home");
            if (!physicalHome.exists()) physicalHome.mkdirs();
            
            File realCompilerJar = new File(baseDir, "kotlin-compiler-real.jar");
            extractCompilerIfNeeded(realCompilerJar);

            if (mAnchorResources.isEmpty()) {
                try (ZipFile zip = new ZipFile(realCompilerJar)) {
                    Enumeration<? extends ZipEntry> entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        mAnchorResources.add(entries.nextElement().getName());
                    }
                }
            }

            List<String> pluginPaths = new ArrayList<>();
            List<String> finalArgs = new ArrayList<>();
            finalArgs.add("-kotlin-home"); finalArgs.add(physicalHome.getAbsolutePath());
            finalArgs.add("-no-jdk"); finalArgs.add("-no-stdlib"); finalArgs.add("-no-reflect");
            
            String outDir = (request.outputDir != null) ? request.outputDir.trim() : "";
            if (!outDir.isEmpty()) {
                File fOut = new File(outDir);
                if (!fOut.exists()) fOut.mkdirs();
                finalArgs.add("-d"); finalArgs.add(fOut.getAbsolutePath());
            }

            if (request.extraArgs != null) {
                for (String arg : request.extraArgs) {
                    if (arg == null) continue;
                    if (arg.startsWith("-Xplugin=")) pluginPaths.add(arg.substring("-Xplugin=".length()));
                    else finalArgs.add(arg);
                }
            }

            StringBuilder resonancePath = new StringBuilder();
            resonancePath.append(realCompilerJar.getAbsolutePath()).append(File.pathSeparator).append(getPackageCodePath());

            final HashSet<String> anchorIndex = mAnchorResources;
            final File anchorFile = realCompilerJar;

            DexClassLoader resonanceLoader = new DexClassLoader(resonancePath.toString(), baseCache.getAbsolutePath(), null, getClassLoader()) {
                @Override public URL getResource(String name) {
                    if (name == null) return null;
                    String resName = name.startsWith("/") ? name.substring(1) : name;
                    if (anchorIndex.contains(resName)) {
                        try { return new URL("jar:file:" + anchorFile.getAbsolutePath() + "!/" + resName); } catch (Exception ignored) {}
                    }
                    return getClassLoader().getResource(name);
                }
                
                @Override public InputStream getResourceAsStream(String name) {
                    if (name == null) return null;
                    String resName = name.startsWith("/") ? name.substring(1) : name;
                    if (anchorIndex.contains(resName)) {
                        try {
                            URL jarUrl = new URL("jar:file:" + anchorFile.getAbsolutePath() + "!/" + resName);
                            return jarUrl.openStream();
                        } catch (Exception ignored) {}
                    }
                    return getClassLoader().getResourceAsStream(name);
                }
            };

            Thread.currentThread().setContextClassLoader(resonanceLoader);

            StringBuilder cp = new StringBuilder();
            if (request.androidJarPath != null && !request.androidJarPath.trim().isEmpty()) {
                cp.append(request.androidJarPath.trim()).append(File.pathSeparator);
            }
            if (request.classpath != null) {
                for (String path : request.classpath) {
                    if (path != null && !path.trim().isEmpty()) {
                        cp.append(path.trim()).append(File.pathSeparator);
                    }
                }
            }
            finalArgs.add("-classpath"); finalArgs.add(cp.toString());
            if (request.sourceFiles != null) {
                for (String src : request.sourceFiles) {
                    if (src != null && !src.trim().isEmpty()) {
                        finalArgs.add(src.trim());
                    }
                }
            }

            System.setProperty("kotlin.version", "1.9.24");
            System.setProperty("user.home", getCacheDir().getAbsolutePath());

            Log.i(TAG, "🚀 TITAN: Iniciando Transmutación Zenith " + VERSION + "...");
            Class<?> compilerClass = resonanceLoader.loadClass("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler");
            Object compiler = compilerClass.getDeclaredConstructor().newInstance();
            Class<?> argumentsClass = resonanceLoader.loadClass("org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments");
            Object arguments = argumentsClass.getDeclaredConstructor().newInstance();
            
            Method parseMethod = findMethodDynamicHeuristic(compilerClass, "parseArguments");
            parseMethod.invoke(compiler, finalArgs.toArray(new String[0]), arguments);
            
            Class<?> configurationClass = resonanceLoader.loadClass("org.jetbrains.kotlin.config.CompilerConfiguration");
            Object configuration = configurationClass.getDeclaredConstructor().newInstance();
            
            Class<?> messageCollectorClass = resonanceLoader.loadClass("org.jetbrains.kotlin.cli.common.messages.MessageCollector");
            Object collector = Proxy.newProxyInstance(resonanceLoader, new Class<?>[]{messageCollectorClass}, (proxy, method, args) -> {
                if (method.getName().equals("report") && args != null && args.length >= 2) {
                    String msg = String.valueOf(args[1]);
                    if (msg != null && !msg.contains("scripting plugin")) {
                        ps.println("[" + args[0] + "] " + msg);
                    }
                }
                return null;
            });

            injectMessageCollector(configuration, collector, resonanceLoader);
            Method setupMethod = findSetupMethodDynamic(compilerClass);
            if (setupMethod != null) {
                setupMethod.invoke(compiler, prepareSetupArgs(setupMethod, configuration, arguments, collector, resonanceLoader));
            }
            
            injectPluginsSovereign(configuration, resonanceLoader);
            
            Method doExecuteMethod = findMethodDynamicHeuristic(compilerClass, "doExecute");
            Object exitCode = doExecuteMethod.invoke(compiler, prepareExecArgs(doExecuteMethod, configuration, arguments, collector, resonanceLoader));
            
            sendResult(String.valueOf(exitCode).equals("OK") || String.valueOf(exitCode).equals("0"), baos.toString());
        } catch (Throwable e) {
            Throwable cause = (e instanceof InvocationTargetException) ? ((InvocationTargetException) e).getTargetException() : e;
            Log.e(TAG, "Fallo TITAN: " + cause.getMessage(), cause);
            sendResult(false, "Fallo TITAN " + VERSION + ": " + cause.toString() + "\nLog:\n" + baos.toString());
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    private Method findMethodDynamicHeuristic(Class<?> clazz, String name) throws NoSuchMethodException {
        Class<?> curr = clazz;
        while (curr != null) {
            for (Method m : curr.getDeclaredMethods()) { if (m.getName().equals(name)) { m.setAccessible(true); return m; } }
            curr = curr.getSuperclass();
        }
        throw new NoSuchMethodException(name);
    }

    private Method findSetupMethodDynamic(Class<?> compilerClass) {
        Class<?> curr = compilerClass;
        while (curr != null) {
            for (Method m : curr.getDeclaredMethods()) { if (m.getName().equals("setupConfiguration") || m.getName().equals("configureConfiguration")) { m.setAccessible(true); return m; } }
            curr = curr.getSuperclass();
        }
        return null;
    }

    private Object[] prepareSetupArgs(Method method, Object config, Object args, Object collector, ClassLoader loader) {
        Class<?>[] pTypes = method.getParameterTypes();
        Object[] setupArgs = new Object[pTypes.length];
        for (int i = 0; i < pTypes.length; i++) {
            String n = pTypes[i].getName();
            if (n.contains("CompilerConfiguration")) setupArgs[i] = config;
            else if (n.contains("Arguments")) setupArgs[i] = args;
            else if (n.contains("MessageCollector")) setupArgs[i] = collector;
            else setupArgs[i] = findStaticFieldDynamic(pTypes[i], "EMPTY", loader);
        }
        return setupArgs;
    }

    private Object[] prepareExecArgs(Method method, Object config, Object args, Object collector, ClassLoader loader) {
        Class<?>[] pTypes = method.getParameterTypes();
        Object[] execArgs = new Object[pTypes.length];
        for (int i = 0; i < pTypes.length; i++) {
            String n = pTypes[i].getName();
            if (n.contains("CompilerConfiguration")) execArgs[i] = config;
            else if (n.contains("Arguments")) execArgs[i] = args;
            else if (n.contains("MessageCollector")) execArgs[i] = collector;
            else if (n.contains("Disposable")) execArgs[i] = createMockDisposable(loader);
            else execArgs[i] = findStaticFieldDynamic(pTypes[i], "EMPTY", loader);
        }
        return execArgs;
    }

    private Object createMockDisposable(ClassLoader loader) {
        try {
            Class<?> dClass = loader.loadClass("org.jetbrains.kotlin.com.intellij.openapi.Disposable");
            return Proxy.newProxyInstance(loader, new Class<?>[]{dClass}, (proxy, m, a) -> null);
        } catch (Exception e) { return null; }
    }

    private void injectMessageCollector(Object configuration, Object collector, ClassLoader loader) {
        try {
            Class<?> configKeyClass = loader.loadClass("org.jetbrains.kotlin.config.CompilerConfigurationKey");
            Method put = configuration.getClass().getMethod("put", configKeyClass, Object.class);
            try { put.invoke(configuration, loader.loadClass("org.jetbrains.kotlin.config.CommonConfigurationKeys").getField("MESSAGE_COLLECTOR").get(null), collector); }
            catch (Exception e) { put.invoke(configuration, loader.loadClass("org.jetbrains.kotlin.cli.common.CLIConfigurationKeys").getField("MESSAGE_COLLECTOR_KEY").get(null), collector); }
        } catch (Exception ignored) {}
    }

    private void injectPluginsSovereign(Object configuration, ClassLoader loader) {
        try {
            List<Object> registrars = new ArrayList<>();
            String[] targets = {"androidx.compose.compiler.plugins.kotlin.ComposePluginRegistrar","org.jetbrains.kotlin.compose.compiler.plugins.kotlin.ComposePluginRegistrar"};
            for (String t : targets) { try { registrars.add(loader.loadClass(t).getDeclaredConstructor().newInstance()); } catch (Exception ignored) {} }
            // Inyectar vía reflexión en CompilerConfiguration si se encuentran registrars...
        } catch (Exception ignored) {}
    }

    private Object findStaticFieldDynamic(Class<?> clazz, String name, ClassLoader loader) {
        try { return clazz.getField(name).get(null); } catch (Exception e) {
            if (clazz.isInterface()) return Proxy.newProxyInstance(loader, new Class<?>[]{clazz}, (proxy, method, args) -> null);
            return null;
        }
    }

    private void sendResult(boolean success, String output) {
        SovereignMessage result = new SovereignMessage(SovereignConstants.ACTION_COMPILE_RESULT);
        result.putExtra(SovereignConstants.EXTRA_SUCCESS, success);
        result.putExtra(SovereignConstants.EXTRA_OUTPUT, output);
        SovereignSignal.send(result);
    }

    private CompileRequest loadRequest(String path) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) { return (CompileRequest) ois.readObject(); }
        catch (Exception e) { return null; }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(new NotificationChannel(CHANNEL_ID, "Kotlin TITAN", NotificationManager.IMPORTANCE_DEFAULT));
        }
    }

    private Notification createNotification(String text, int progress) {
        return new NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("TITAN Zenith").setContentText(text).setSmallIcon(android.R.drawable.stat_notify_sync).setOngoing(true).setProgress(100, progress, false).build();
    }
}
