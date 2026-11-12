package com.tyron.code.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.android.tools.r8.D8;
import com.android.tools.r8.D8Command;
import com.android.tools.r8.OutputMode;
import com.tyron.builder.SovereignConstants;
import com.tyron.builder.SovereignMessage;
import com.tyron.builder.SovereignSignal;
import com.tyron.builder.model.CompileRequest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import dalvik.system.PathClassLoader;

/**
 * KotlinService v2026.900: "The Neural Singularity - Total Alignment Zenith".
 * v2026.900: Sincronía total de firmas moleculares y eliminación de sombras de ClassLoader.
 * v2026.885: Sincronía total de firmas moleculares.
 */
public class KotlinService extends Service {
    
    private static final String TAG = "KotlinService";
    private static final String VERSION = "v2026.900";
    private static final String CHANNEL_ID = "kotlin_titan_singularity_2026";
    private static final int NOTIFICATION_ID = 2026;

    private PowerManager.WakeLock mWakeLock;
    private final ScheduledExecutorService mHeartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
    private int mHeartbeatCount = 0;

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
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
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } catch (Exception e) {
                startForeground(NOTIFICATION_ID, notification);
            }
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void handleCompileRequest(Intent intent) {
        String requestPath = intent.getStringExtra("request_file");
        new Thread(() -> {
            try {
                CompileRequest request = loadRequest(requestPath);
                if (request != null) {
                    mHeartbeatExecutor.scheduleWithFixedDelay(this::performPulse, 1, 5, TimeUnit.SECONDS);
                    performKotlinCompilation(request);
                }
            } catch (Exception e) {
                Log.e(TAG, "Fallo TITAN K2: " + e.getMessage(), e);
                sendResult(false, "Excepción Crítica TITAN K2: " + e.getMessage());
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
        SovereignMessage msg = new SovereignMessage(SovereignConstants.ACTION_HEARTBEAT);
        SovereignSignal.send(msg);
    }

    private File autoDexPlugin(File rawJar) throws Exception {
        File dexedJar = new File(getCodeCacheDir(), "dexed_" + rawJar.getName());
        if (dexedJar.exists() && dexedJar.lastModified() >= rawJar.lastModified() && dexedJar.length() > 1000) {
            applySovereignLock(dexedJar);
            return dexedJar;
        }
        
        Log.i(TAG, "🔮 TITAN: Transmutando plugin " + rawJar.getName() + " a materia Dalvik...");
        if (dexedJar.exists()) dexedJar.setWritable(true);
        
        D8Command command = D8Command.builder()
                .addProgramFiles(rawJar.toPath())
                .setOutput(dexedJar.toPath(), OutputMode.DexIndexed)
                .setDisableDesugaring(true)
                .build();
        D8.run(command);
        
        applySovereignLock(dexedJar);
        return dexedJar;
    }

    private void applySovereignLock(File file) {
        if (file == null || !file.exists()) return;
        try {
            file.setWritable(false, false);
            file.setReadOnly();
            Runtime.getRuntime().exec("chmod 444 " + file.getAbsolutePath()).waitFor();
            Log.i(TAG, "🔒 Sello Inmutable (444) aplicado a: " + file.getName());
        } catch (Exception e) {
            Log.w(TAG, "⚠️ Error en sellado: " + e.getMessage());
        }
    }

    private void performKotlinCompilation(CompileRequest request) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        try {
            File baseDir = new File(getCodeCacheDir(), "kotlin_zenith_core");
            if (!baseDir.exists()) baseDir.mkdirs();
            File physicalHome = new File(baseDir, "kotlin_home");
            
            List<String> pluginPaths = new ArrayList<>();
            List<String> finalArgs = new ArrayList<>();
            finalArgs.add("-kotlin-home"); finalArgs.add(physicalHome.getAbsolutePath());
            finalArgs.add("-no-jdk"); finalArgs.add("-no-stdlib"); finalArgs.add("-no-reflect");
            finalArgs.add("-d"); finalArgs.add(request.outputDir);

            for (String arg : request.extraArgs) {
                if (arg.startsWith("-Xplugin=")) {
                    pluginPaths.add(arg.substring("-Xplugin=".length()));
                } else {
                    finalArgs.add(arg);
                }
            }

            StringBuilder resonancePath = new StringBuilder();
            File compilerJar = new File(baseDir, "kotlin-compiler-anchor-" + VERSION + ".jar");
            prepareCompilerAnchor(compilerJar, physicalHome);
            
            // PRIORIDAD SOBERANA: El APK (con nuestros stubs saneados) va ANTES.
            resonancePath.append(getPackageCodePath());
            
            for (String p : pluginPaths) {
                try {
                    File dexed = autoDexPlugin(new File(p));
                    resonancePath.append(File.pathSeparator).append(dexed.getAbsolutePath());
                } catch (Exception e) { Log.e(TAG, "Error dexing plugin " + p + ": " + e.getMessage()); }
            }

            PathClassLoader resonanceLoader = new PathClassLoader(resonancePath.toString(), getClassLoader().getParent()) {
                @Override
                public URL getResource(String name) {
                    String resName = name.startsWith("/") ? name.substring(1) : name;
                    if (resName.startsWith("org/jetbrains/kotlin/") || resName.contains(".kotlin_builtins") || resName.contains(".xml") || resName.contains("META-INF/")) {
                         try { 
                             URL url = new URL("jar:file:" + compilerJar.getAbsolutePath() + "!/" + resName);
                             url.openConnection().getInputStream().close();
                             return url;
                         } catch (Exception ignored) {}
                    }
                    if (resName.contains("PathUtil.class")) {
                        try { return new File(physicalHome, resName).toURI().toURL(); } catch (Exception ignored) {}
                    }
                    return super.getResource(name);
                }
                
                @Override
                protected Class<?> findClass(String name) throws ClassNotFoundException {
                    try {
                        return super.findClass(name);
                    } catch (UnsupportedOperationException | ClassCastException e) {
                        throw new ClassNotFoundException("Blindaje TITAN: Normalizando error de ClassLoader para " + name, e);
                    }
                }
            };
            
            Thread.currentThread().setContextClassLoader(resonanceLoader);

            StringBuilder cp = new StringBuilder();
            if (request.androidJarPath != null && new File(request.androidJarPath).exists()) cp.append(request.androidJarPath).append(File.pathSeparator);
            if (request.classpath != null) {
                for (String path : request.classpath) {
                    if (!path.endsWith("base.apk") && !path.contains(getPackageName())) cp.append(path).append(File.pathSeparator);
                }
            }
            finalArgs.add("-classpath"); finalArgs.add(cp.toString());
            for (String src : request.sourceFiles) { if (new File(src).exists()) finalArgs.add(src); }

            System.setProperty("kotlin.scripting.compiler.plugin.disabled", "true");
            System.setProperty("idea.io.use.nio2", "true");
            System.setProperty("kotlin.version", "1.9.24");
            System.setProperty("idea.is.internal", "false");
            System.setProperty("user.home", getCacheDir().getAbsolutePath());

            Log.i(TAG, "🚀 TITAN: Iniciando Transmutación Zenith " + VERSION + "...");
            
            Class<?> compilerClass = resonanceLoader.loadClass("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler");
            Object compiler = compilerClass.getDeclaredConstructor().newInstance();
            
            Class<?> argumentsClass = resonanceLoader.loadClass("org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments");
            Object arguments = argumentsClass.getDeclaredConstructor().newInstance();
            
            Method parseMethod = findMethodDynamicHeuristic(compilerClass, "parseArguments");
            parseMethod.invoke(compiler, finalArgs.toArray(new String[0]), arguments);
            
            clearPluginArguments(arguments);
            
            Class<?> configurationClass = resonanceLoader.loadClass("org.jetbrains.kotlin.config.CompilerConfiguration");
            Object configuration = configurationClass.getDeclaredConstructor().newInstance();
            
            Class<?> messageCollectorClass = resonanceLoader.loadClass("org.jetbrains.kotlin.cli.common.messages.MessageCollector");
            Object collector = findStaticFieldDynamic(messageCollectorClass, "NONE", resonanceLoader);
            
            injectMessageCollector(configuration, collector, resonanceLoader);

            Method setupMethod = findSetupMethodDynamic(compilerClass);
            if (setupMethod != null) {
                Object[] setupArgs = prepareSetupArgs(setupMethod, configuration, arguments, collector, resonanceLoader);
                setupMethod.invoke(compiler, setupArgs);
            }

            injectPluginsSovereign(configuration, resonanceLoader);
            
            Method doExecuteMethod = findMethodDynamicHeuristic(compilerClass, "doExecute");
            Object[] execArgs = prepareExecArgs(doExecuteMethod, configuration, arguments, collector, resonanceLoader);
            
            Log.i(TAG, "🎯 Mapeo Molecular: Disparando doExecute con firmas de método Zenith...");
            Object exitCode = doExecuteMethod.invoke(compiler, execArgs);
            
            String logResult = baos.toString();
            boolean success = String.valueOf(exitCode).equals("OK") || String.valueOf(exitCode).equals("0");
            
            sendResult(success, logResult);
            
        } catch (Throwable e) {
            Throwable cause = (e instanceof InvocationTargetException) ? ((InvocationTargetException) e).getTargetException() : e;
            Log.e(TAG, "Fallo Crítico TITAN: " + cause.getMessage(), cause);
            
            StringBuilder sb = new StringBuilder();
            sb.append("Fallo TITAN ").append(VERSION).append(": ").append(cause.toString()).append("\n");
            Throwable root = cause.getCause();
            while (root != null) {
                sb.append("Causado por: ").append(root.toString()).append("\n");
                root = root.getCause();
            }
            sendResult(false, sb.toString() + "\nLog:\n" + baos.toString());
        }
    }

    private void clearPluginArguments(Object arguments) {
        try {
            Class<?> curr = arguments.getClass();
            while (curr != null) {
                for (Field f : curr.getDeclaredFields()) {
                    String name = f.getName();
                    if (name.equals("pluginClasspaths") || name.equals("pluginOptions") || name.equals("pluginConfigurations")) {
                        f.setAccessible(true); f.set(arguments, null);
                    }
                }
                curr = curr.getSuperclass();
            }
        } catch (Exception ignored) {}
    }

    private void prepareCompilerAnchor(File compilerJar, File physicalHome) throws IOException {
        long lastUpdate = 0;
        try { lastUpdate = getPackageManager().getPackageInfo(getPackageName(), 0).lastUpdateTime; } catch (Exception ignored) {}
        if (!compilerJar.exists() || compilerJar.lastModified() < lastUpdate) {
            Log.i(TAG, "🔥 TITAN: Reconstruyendo Anclaje de Símbolos Zenith v2026.900...");
            if (compilerJar.exists()) compilerJar.setWritable(true);
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(compilerJar))) {
                String[] anchors = {
                    "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler",
                    "org.jetbrains.kotlin.cli.common.CLITool",
                    "org.jetbrains.kotlin.utils.PathUtil",
                    "org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment",
                    "org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment$Companion"
                };
                for (String anchor : anchors) {
                    zos.putNextEntry(new ZipEntry(anchor.replace('.', '/') + ".class"));
                    zos.write(new byte[]{(byte)0xCA, (byte)0xFE, (byte)0xBA, (byte)0xBE, 0, 0, 0, 52, 0, 0, 0, 0});
                    zos.closeEntry();
                }
                try (ZipFile apk = new ZipFile(getPackageCodePath())) {
                    Enumeration<? extends ZipEntry> entries = apk.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry ze = entries.nextElement();
                        String name = ze.getName();
                        if (name.startsWith("META-INF/") || name.endsWith(".xml") || name.endsWith(".kotlin_builtins")) {
                            zos.putNextEntry(new ZipEntry(name));
                            try (java.io.InputStream is = apk.getInputStream(ze)) {
                                byte[] buf = new byte[8192]; int len;
                                while ((len = is.read(buf)) > 0) zos.write(buf, 0, len);
                            }
                            zos.closeEntry();
                        }
                    }
                }
            }
            applySovereignLock(compilerJar);
        }
        
        File pathUtilDir = new File(physicalHome, "org/jetbrains/kotlin/utils");
        pathUtilDir.mkdirs();
        File pathUtilFile = new File(pathUtilDir, "PathUtil.class");
        if (!pathUtilFile.exists()) {
            try (FileOutputStream fos = new FileOutputStream(pathUtilFile)) {
                fos.write(new byte[] {(byte)0xCA, (byte)0xFE, (byte)0xBA, (byte)0xBE, 0, 0, 0, 52, 0, 10, 7, 0, 2, 1, 0, 6, 60, 105, 110, 105, 116, 62, 1, 0, 3, 40, 41, 86, 1, 0, 4, 67, 111, 100, 101, 10, 0, 1, 0, 3, 12, 0, 5, 0, 6, 1, 0, 16, 106, 97, 118, 97, 47, 108, 97, 110, 103, 47, 79, 98, 106, 101, 99, 116, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 2, 0, 3, 0, 1, 0, 4, 0, 0, 0, 17, 0, 1, 0, 1, 0, 0, 0, 5, 42, (byte)183, 0, 5, (byte)177, 0, 0, 0, 0, 0, 0});
            }
        }
    }

    private void injectMessageCollector(Object configuration, Object collector, ClassLoader loader) {
        try {
            Object key = null;
            try { key = loader.loadClass("org.jetbrains.kotlin.config.CommonConfigurationKeys").getField("MESSAGE_COLLECTOR").get(null); }
            catch (Exception e) { key = loader.loadClass("org.jetbrains.kotlin.cli.common.CLIConfigurationKeys").getField("MESSAGE_COLLECTOR_KEY").get(null); }
            if (key != null) {
                Method put = configuration.getClass().getMethod("put", loader.loadClass("org.jetbrains.kotlin.config.CompilerConfigurationKey"), Object.class);
                put.invoke(configuration, key, collector);
            }
        } catch (Exception e) { Log.w(TAG, "Fallo inyectando MessageCollector: " + e.getMessage()); }
    }

    private Method findSetupMethodDynamic(Class<?> compilerClass) {
        Class<?> current = compilerClass;
        while (current != null) {
            for (Method m : current.getDeclaredMethods()) {
                if (m.getName().equals("setupConfiguration") || m.getName().equals("configureConfiguration")) {
                    m.setAccessible(true); return m;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private Object[] prepareSetupArgs(Method method, Object config, Object args, Object collector, ClassLoader loader) {
        Class<?>[] pTypes = method.getParameterTypes();
        Object[] setupArgs = new Object[pTypes.length];
        for (int i = 0; i < pTypes.length; i++) {
            String name = pTypes[i].getName();
            if (name.contains("CompilerConfiguration")) setupArgs[i] = config;
            else if (name.contains("Arguments")) setupArgs[i] = args;
            else if (name.contains("MessageCollector")) setupArgs[i] = collector;
            else if (name.contains("Services")) setupArgs[i] = findStaticFieldDynamic(pTypes[i], "EMPTY", loader);
            else setupArgs[i] = null;
        }
        return setupArgs;
    }

    private Object[] prepareExecArgs(Method method, Object config, Object args, Object collector, ClassLoader loader) {
        Class<?>[] pTypes = method.getParameterTypes();
        Object[] execArgs = new Object[pTypes.length];
        for (int i = 0; i < pTypes.length; i++) {
            String name = pTypes[i].getName();
            if (name.contains("CompilerConfiguration")) execArgs[i] = config;
            else if (name.contains("Arguments")) execArgs[i] = args;
            else if (name.contains("MessageCollector")) execArgs[i] = collector;
            else if (name.contains("Services")) execArgs[i] = findStaticFieldDynamic(pTypes[i], "EMPTY", loader);
            else if (name.contains("Disposable")) execArgs[i] = createMockDisposable(loader);
            else execArgs[i] = null;
        }
        return execArgs;
    }

    private Object createMockDisposable(ClassLoader loader) {
        try {
            Class<?> disposableClass = loader.loadClass("org.jetbrains.kotlin.com.intellij.openapi.Disposable");
            return Proxy.newProxyInstance(loader, new Class<?>[]{disposableClass}, (proxy, method, args) -> null);
        } catch (Exception e) { return null; }
    }

    private void injectPluginsSovereign(Object configuration, ClassLoader loader) {
        try {
            List<Object> registrars = new ArrayList<>();
            String[] targets = {
                "androidx.compose.compiler.plugins.kotlin.ComposePluginRegistrar",
                "androidx.compose.compiler.plugins.kotlin.ComposeCompilerPluginRegistrar",
                "androidx.compose.compiler.plugins.kotlin.ComposeComponentRegistrar"
            };
            for (String t : targets) {
                try { registrars.add(loader.loadClass(t).getDeclaredConstructor().newInstance()); }
                catch (Exception ignored) {}
            }
            if (registrars.isEmpty()) return;
            
            injectIntoKey(configuration, loader, "org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar", "PLUGIN_COMPONENT_REGISTRARS", registrars);
            injectIntoKey(configuration, loader, "org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar", "COMPILER_PLUGIN_REGISTRARS", registrars);
        } catch (Exception e) { Log.e(TAG, "Fallo en Inyección Soberana: " + e.getMessage()); }
    }

    private void injectIntoKey(Object configuration, ClassLoader loader, String keyClassName, String fieldName, List<Object> registrars) {
        try {
            Class<?> keyHolder = loader.loadClass(keyClassName);
            Object key = null;
            try { Field f = keyHolder.getDeclaredField(fieldName); f.setAccessible(true); key = f.get(null); }
            catch (Exception e) {
                try {
                    Field comp = keyHolder.getDeclaredField("Companion"); comp.setAccessible(true); Object companion = comp.get(null);
                    Field f = companion.getClass().getDeclaredField(fieldName); f.setAccessible(true); key = f.get(companion);
                } catch (Exception ignored) {}
            }
            if (key != null) {
                Method addAll = null;
                for (Method m : configuration.getClass().getMethods()) {
                    if (m.getName().equals("addAll") && m.getParameterTypes().length == 2) { addAll = m; break; }
                }
                if (addAll != null) addAll.invoke(configuration, key, registrars);
            }
        } catch (Exception ignored) {}
    }

    private Method findMethodDynamicHeuristic(Class<?> clazz, String name) throws NoSuchMethodException {
        Class<?> current = clazz;
        while (current != null) {
            for (Method m : current.getDeclaredMethods()) {
                if (m.getName().equals(name)) { m.setAccessible(true); return m; }
            }
            current = current.getSuperclass();
        }
        throw new NoSuchMethodException(name + " no hallado en " + clazz.getName());
    }

    private Object findStaticFieldDynamic(Class<?> clazz, String name, ClassLoader loader) {
        try { return clazz.getField(name).get(null); }
        catch (Exception e) {
            try {
                Object companion = clazz.getField("Companion").get(null);
                try { return companion.getClass().getField(name).get(companion); }
                catch (Exception e2) { return companion.getClass().getMethod("get" + name).invoke(companion); }
            } catch (Exception e3) {
                if (clazz.isInterface()) return Proxy.newProxyInstance(loader, new Class<?>[]{clazz}, (proxy, method, args) -> null);
            }
        }
        return null;
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
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Kotlin TITAN", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification createNotification(String text, int progress) {
        return new NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("TITAN Zenith").setContentText(text).setSmallIcon(android.R.drawable.stat_notify_sync).setOngoing(true).setProgress(100, progress, false).build();
    }
}
