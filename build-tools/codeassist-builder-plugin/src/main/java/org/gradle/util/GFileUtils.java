package org.gradle.util;

import java.io.File;

/**
 * Bridge class to satisfy legacy Gradle API usage in Gradle 8.x
 * Restored required methods for CodeAssist.
 */
public class GFileUtils {

    public static String toSystemIndependentPath(String path) {
        return path == null ? null : path.replace('\\', '/');
    }

    public static File join(File base, String... segments) {
        File result = base;
        for (String segment : segments) {
            result = new File(result, segment);
        }
        return result;
    }

    public static File join(File base, String segment) {
        return new File(base, segment);
    }
    
    public static File join(File base, String segment1, String segment2) {
        return new File(new File(base, segment1), segment2);
    }

    public static void mkdirs(File dir) {
        if (!dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory()) {
            throw new RuntimeException("Cannot create directory: " + dir);
        }
    }
}
