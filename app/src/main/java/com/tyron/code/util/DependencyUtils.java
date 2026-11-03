package com.tyron.code.util;

import android.content.Context;
import android.os.Environment;
import com.tyron.builder.log.ILogger;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.util.*;

/**
 * DependencyUtils v34.23: "Total Hygiene".
 * Implementa sincronización inteligente y purga agresiva de librerías obsoletas
 * que causan colisiones en el Dexer (D8).
 */
public class DependencyUtils {

    public static void downloadDependencies(Context context, File projectRoot, ILogger logger, Runnable onComplete) {
        logger.info("------------------------------------------");
        logger.info("🛠️ INICIANDO SYNC TOTAL (v34.23): " + projectRoot.getName());
        
        File repoDir = null;
        String[] paths = {"Download/CodeAssistprojects/repo", "Download/CodeAssistProjects/repo"};
        for (String p : paths) {
            File d = new File(Environment.getExternalStorageDirectory(), p);
            if (d.exists()) {
                repoDir = d;
                break;
            }
        }

        if (repoDir == null) {
            logger.error("❌ ERROR: Repositorio no encontrado.");
            if (onComplete != null) onComplete.run();
            return;
        }

        File libsDir = new File(projectRoot, "app/libs");
        if (!libsDir.exists()) libsDir = new File(projectRoot, "libs");
        if (!libsDir.exists()) libsDir.mkdirs();

        int updated = 0;
        int skipped = 0;
        int purged = 0;

        try {
            // 1. PURGA DE LINAJES OBSOLETOS (Anti-D8 Collision)
            File[] existingLibs = libsDir.listFiles();
            if (existingLibs != null) {
                for (File lib : existingLibs) {
                    String name = lib.getName().toLowerCase();
                    boolean isObsolete = name.contains("kotlin-stdlib-jdk7") || 
                                       name.contains("kotlin-stdlib-jdk8") ||
                                       name.equals("annotations-13.0.jar"); // PURGA CRÍTICA
                    
                    if (isObsolete) {
                        lib.delete();
                        purged++;
                    }
                }
            }

            // 2. SINCRONIZACIÓN INCREMENTAL FILTRADA
            File[] files = repoDir.listFiles();
            if (files != null) {
                for (File repoFile : files) {
                    if (repoFile.isDirectory() || (!repoFile.getName().endsWith(".jar") && !repoFile.getName().endsWith(".aar"))) continue;
                    
                    // Bloqueo preventivo en el repo
                    String name = repoFile.getName().toLowerCase();
                    if (name.contains("jdk7") || name.contains("jdk8") || name.equals("annotations-13.0.jar")) continue;
                    
                    if (repoFile.length() < 1000) continue;

                    File target = new File(libsDir, repoFile.getName());
                    if (target.exists() && target.length() == repoFile.length()) {
                        skipped++;
                        continue;
                    }

                    FileUtils.copyFile(repoFile, target);
                    updated++;
                }
            }
        } catch (Exception e) { logger.error("❌ Error en sync: " + e.getMessage()); }

        logger.info("✅ SYNC COMPLETADO.");
        if (purged > 0) logger.info("🧹 Purga de archivos redundantes: " + purged);
        logger.info("📦 Actualizados: " + updated + " | ⏭️ Omitidos: " + skipped);
        logger.info("------------------------------------------");
        if (onComplete != null) onComplete.run();
    }

    private static String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new java.text.DecimalFormat("#,##0.#").format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
