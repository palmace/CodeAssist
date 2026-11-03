package com.tyron.builder.gradle.internal;

import com.android.annotations.NonNull;
import com.android.ide.common.resources.MergingException;
import com.tyron.builder.model.DiagnosticWrapper;

import java.io.Serializable;
import java.util.Locale;
import java.util.function.Supplier;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation that bridges both Android's ILogger and CodeAssist's ILogger
 */
public class LoggerWrapper implements com.android.utils.ILogger, com.tyron.builder.log.ILogger {

    private final Logger logger;

    @NonNull
    public static LoggerWrapper getLogger(@NonNull Class<?> klass) {
        return new LoggerWrapper(Logging.getLogger(klass));
    }

    public LoggerWrapper(@NonNull Logger logger) {
        this.logger = logger;
    }

    // --- com.tyron.builder.log.ILogger implementation ---

    @Override
    public void info(DiagnosticWrapper wrapper) {
        log(LogLevel.INFO, wrapper.getMessage(Locale.US).toString(), null);
    }

    @Override
    public void debug(DiagnosticWrapper wrapper) {
        log(LogLevel.DEBUG, wrapper.getMessage(Locale.US).toString(), null);
    }

    @Override
    public void warning(DiagnosticWrapper wrapper) {
        log(LogLevel.WARN, wrapper.getMessage(Locale.US).toString(), null);
    }

    @Override
    public void error(DiagnosticWrapper wrapper) {
        log(LogLevel.ERROR, wrapper.getMessage(Locale.US).toString(), null);
    }

    @Override
    public void quiet(String s) {
        log(LogLevel.QUIET, s, null);
    }

    @Override
    public void lifecycle(String s) {
        log(LogLevel.LIFECYCLE, s, null);
    }

    @Override
    public void verbose(String s) {
        log(LogLevel.DEBUG, s, null);
    }

    // --- com.android.utils.ILogger implementation ---

    @Override
    public void error(@Nullable Throwable throwable, @Nullable String s, Object... objects) {
        if (throwable instanceof MergingException) {
            return;
        }
        if (s == null) {
            s = throwable != null ? throwable.getMessage() : "[no message]";
        } else if (objects != null && objects.length > 0) {
            s = String.format(s, objects);
        }
        if (throwable == null) {
            logger.error(s);
        } else {
            logger.error(s, throwable);
        }
    }

    @Override
    public void warning(@NonNull String s, Object... objects) {
        log(LogLevel.WARN, s, objects);
    }

    @Override
    public void info(@NonNull String s, Object... objects) {
        log(LogLevel.INFO, s, objects);
    }

    @Override
    public void verbose(@NonNull String s, Object... objects) {
        log(LogLevel.DEBUG, s, objects);
    }

    @Override
    public void quiet(@NonNull String s, Object... objects) {
        log(LogLevel.QUIET, s, objects);
    }

    @Override
    public void lifecycle(@NonNull String s, Object... objects) {
        log(LogLevel.LIFECYCLE, s, objects);
    }

    // Helper
    private void log(@NonNull LogLevel logLevel, @NonNull String s, @Nullable Object[] objects) {
        if (!logger.isEnabled(logLevel)) {
            return;
        }
        if (objects == null || objects.length == 0) {
            logger.log(logLevel, s);
        } else {
            logger.log(logLevel, String.format(s, objects));
        }
    }

    // Bridge for error(Throwable, String) from com.tyron.builder.log.ILogger
    @Override
    public void error(@Nullable Throwable t, @Nullable String message) {
        error(t, message, (Object[]) null);
    }

    public static Supplier<com.android.utils.ILogger> supplierFor(Class<?> c) {
        return new LoggerSupplier(c);
    }

    private static class LoggerSupplier implements Supplier<com.android.utils.ILogger>, Serializable {
        private final Class<?> clazz;
        private transient com.android.utils.ILogger logger = null;

        private LoggerSupplier(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public synchronized com.android.utils.ILogger get() {
            if (logger == null) {
                logger = new LoggerWrapper(Logging.getLogger(clazz));
            }
            return logger;
        }
    }
}
