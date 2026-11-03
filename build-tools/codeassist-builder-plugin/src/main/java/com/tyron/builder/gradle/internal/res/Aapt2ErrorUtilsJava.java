package com.tyron.builder.gradle.internal.res;

import com.tyron.builder.log.ILogger;

/**
 * Java helper for AAPT2 error utilities to bypass Kotlin visibility restrictions.
 */
public class Aapt2ErrorUtilsJava {

    public static BlameLoggerWrapper blameLoggerFor(ILogger logger) {
        return new BlameLoggerWrapper(logger, source -> source);
    }
}
