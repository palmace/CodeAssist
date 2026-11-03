package com.android.tools.aapt2;

import android.text.TextUtils;
import android.util.Log;
import com.tyron.builder.BuildModule;
import com.tyron.builder.log.ILogger;
import com.tyron.builder.model.DiagnosticWrapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.tools.Diagnostic;

/**
 * Aapt2Jni v44.0.6: "THE ZENITH RADIANCE - IMMUTABLE ANCHOR".
 * v44.0.6: Blindaje Nivel 13 - Aplicación de Sello Inmutable (chmod 444) para Android 14+.
 * v44.0.6: Erradicación de bloqueos de ejecución en HyperOS/Android 14.
 */
public class Aapt2Jni {
    private static final String TAG = "Aapt2Jni";
    private static final Pattern DIAGNOSTIC_PATTERN = Pattern.compile("(.*?):(\\d+): (.*?): (.+)");
    
    private static final Aapt2Jni INSTANCE = new Aapt2Jni();
    public static Aapt2Jni getInstance() { return INSTANCE; }

    private final List<DiagnosticWrapper> mDiagnostics = new ArrayList<>();

    public static int compile(List<String> args, ILogger logger) {
        return runCommand("compile", args, logger);
    }

    public static int link(List<String> args, ILogger logger) {
        return runCommand("link", args, logger);
    }

    private static int runCommand(String cmd, List<String> args, ILogger logger) {
        getInstance().mDiagnostics.clear();
        String binaryPath = getBinary();
        
        File binFile = new File(binaryPath);
        if (!binFile.exists() || !binFile.canExecute()) {
             binaryPath = rescueBinary();
        }

        List<String> command = new ArrayList<>();
        command.add(binaryPath);
        command.add(cmd);
        command.addAll(args);
        return executeBinary(command, logger);
    }

    public static String getBinary() {
        File internalBin = new File(BuildModule.getContext().getFilesDir(), "bin/aapt2_sovereign");
        if (internalBin.exists() && internalBin.canExecute()) {
            return internalBin.getAbsolutePath();
        }
        
        String bundledBinary = BuildModule.getContext().getApplicationInfo().nativeLibraryDir + "/libaapt2.so";
        File bundled = new File(bundledBinary);
        if (bundled.exists() && bundled.canExecute()) {
            return bundledBinary;
        }
        
        return internalBin.getAbsolutePath();
    }

    private static String rescueBinary() {
        File target = new File(BuildModule.getContext().getFilesDir(), "bin/aapt2_sovereign");
        try {
            if (target.exists()) {
                target.setWritable(true); // Temporal para actualización
            }
            target.getParentFile().mkdirs();
            
            String bundledBinary = BuildModule.getContext().getApplicationInfo().nativeLibraryDir + "/libaapt2.so";
            File source = new File(bundledBinary);
            
            if (source.exists()) {
                Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                extractBinaryFromApk(target);
            }
            
            // Blindaje Nivel 13: Sello Inmutable
            target.setExecutable(true, false);
            target.setWritable(false, false);
            target.setReadOnly();
            Runtime.getRuntime().exec("chmod 444 " + target.getAbsolutePath()).waitFor();
            
            Log.i(TAG, "🔥 TITAN: Binario AAPT2 sellado e inmutable en: " + target.getAbsolutePath());
            return target.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Fallo crítico en el sellado Zenith de AAPT2: " + e.getMessage());
            return new File(BuildModule.getContext().getCodeCacheDir(), "libaapt2.so").getAbsolutePath();
        }
    }

    private static void extractBinaryFromApk(File target) throws IOException {
        String apkPath = BuildModule.getContext().getApplicationInfo().sourceDir;
        try (ZipFile zipFile = new ZipFile(apkPath)) {
            ZipEntry entry = null;
            String[] supportedAbis = getSupportedAbis();
            for (String abi : supportedAbis) {
                String zipEntryPath = "lib/" + abi + "/libaapt2.so";
                entry = zipFile.getEntry(zipEntryPath);
                if (entry != null) break;
            }
            
            if (entry == null) throw new IOException("No se encontró libaapt2.so.");
            
            try (InputStream is = zipFile.getInputStream(entry)) {
                Files.copy(is, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static String[] getSupportedAbis() {
        try {
            return (String[]) Class.forName("android.os.Build").getField("SUPPORTED_ABIS").get(null);
        } catch (Exception e) {
            return new String[]{"arm64-v8a", "armeabi-v7a", "armeabi"};
        }
    }

    private static int executeBinary(List<String> args, ILogger externalLogger) {
        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.redirectErrorStream(true);
            pb.environment().put("LD_LIBRARY_PATH", new File(args.get(0)).getParent());
            
            Process process = pb.start();
            java.util.Scanner scanner = new java.util.Scanner(process.getInputStream());
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (externalLogger != null) {
                    if (line.toLowerCase().contains("error:")) externalLogger.error(line);
                    else if (line.toLowerCase().contains("warning:")) externalLogger.warning(line);
                    else externalLogger.info(line);
                }
                parseLine(line);
            }
            process.waitFor();
        } catch (Exception e) {
            if (externalLogger != null) externalLogger.error("Error de ejecución AAPT2 (Zenith): " + e.getMessage());
            return 1;
        }
        return getInstance().mDiagnostics.stream().anyMatch(it -> it.getKind() == Diagnostic.Kind.ERROR) ? 1 : 0;
    }

    private static void parseLine(String line) {
        Matcher m = DIAGNOSTIC_PATTERN.matcher(line);
        if (m.find()) {
            DiagnosticWrapper wrapper = new DiagnosticWrapper();
            wrapper.setKind(line.toLowerCase().contains("error") ? Diagnostic.Kind.ERROR : Diagnostic.Kind.WARNING);
            wrapper.setSource(new File(m.group(1)));
            try { wrapper.setLineNumber(Long.parseLong(m.group(2))); } catch (NumberFormatException ignored) {}
            wrapper.setMessage(m.group(4));
            getInstance().mDiagnostics.add(wrapper);
        } else if (line.toLowerCase().contains("error:")) {
            DiagnosticWrapper wrapper = new DiagnosticWrapper();
            wrapper.setKind(Diagnostic.Kind.ERROR);
            wrapper.setMessage(line);
            getInstance().mDiagnostics.add(wrapper);
        }
    }

    public static File getSymlinkedAapt2Directory() { return new File(getBinary()).getParentFile(); }
    public static List<DiagnosticWrapper> getLogs() { return getInstance().mDiagnostics; }
}
