package com.tyron.builder.gradle.internal.dependency;

import static com.android.SdkConstants.FD_JARS;
import static com.android.SdkConstants.FN_CLASSES_JAR;

import org.gradle.util.GFileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Utility methods for AAR transformation. */
public class AarTransformUtil {

    @NotNull
    public static List<File> getJars(@NotNull File input) {
        List<File> jars = new ArrayList<>();

        File file = GFileUtils.join(input, FD_JARS, FN_CLASSES_JAR);
        if (file.exists()) {
            jars.add(file);
        }

        File libs = new File(input, FD_JARS + "/libs");
        if (libs.isDirectory()) {
            File[] files = libs.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && f.getName().endsWith(".jar")) {
                        jars.add(f);
                    }
                }
            }
        }

        return jars;
    }
}
