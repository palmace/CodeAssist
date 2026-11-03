package com.tyron.builder.log;

import com.tyron.builder.model.DiagnosticWrapper;

import javax.tools.Diagnostic;

public interface ILogger {

    ILogger STD_OUT = new ILogger() {
        @Override
        public void info(DiagnosticWrapper wrapper) {
            System.out.println("[INFO] " + wrapper.toString());
        }

        @Override
        public void debug(DiagnosticWrapper wrapper) {
            System.out.println("[DEBUG] " + wrapper.toString());
        }

        @Override
        public void warning(DiagnosticWrapper wrapper) {
            System.out.println("[WARNING] " + wrapper.toString());
        }

        @Override
        public void error(DiagnosticWrapper wrapper) {
            System.out.println("[ERROR] " + wrapper.toString());
        }

        @Override
        public void quiet(String s) {
             System.out.println("[QUIET] " + s);
        }
        
        @Override
        public void lifecycle(String s) {
             System.out.println("[LIFECYCLE] " + s);
        }
        
        @Override
        public void verbose(String s) {
             System.out.println("[VERBOSE] " + s);
        }
    };

    ILogger EMPTY = new ILogger() {
        @Override
        public void info(DiagnosticWrapper wrapper) {}
        @Override
        public void debug(DiagnosticWrapper wrapper) {}
        @Override
        public void warning(DiagnosticWrapper wrapper) {}
        @Override
        public void error(DiagnosticWrapper wrapper) {}
        @Override
        public void quiet(String s) {}
        @Override
        public void lifecycle(String s) {}
        @Override
        public void verbose(String s) {}
    };

    static ILogger wrap(LogViewModel logViewModel) {
        return new ILogger() {
            @Override
            public void info(DiagnosticWrapper wrapper) {
                logViewModel.d(LogViewModel.BUILD_LOG, wrapper);
            }

            @Override
            public void debug(DiagnosticWrapper wrapper) {
                logViewModel.d(LogViewModel.BUILD_LOG, wrapper);
            }

            @Override
            public void warning(DiagnosticWrapper wrapper) {
                logViewModel.w(LogViewModel.BUILD_LOG, wrapper);
            }

            @Override
            public void error(DiagnosticWrapper wrapper) {
                logViewModel.e(LogViewModel.BUILD_LOG, wrapper);
            }

            @Override
            public void quiet(String s) {
                logViewModel.d(LogViewModel.BUILD_LOG, wrap(s));
            }
            
            @Override
            public void lifecycle(String s) {
                logViewModel.d(LogViewModel.BUILD_LOG, wrap(s)); // Usando d como fallback si i no existe
            }
            
            @Override
            public void verbose(String s) {
                logViewModel.d(LogViewModel.BUILD_LOG, wrap(s));
            }
        };
    }
    
    void info(DiagnosticWrapper wrapper);

    void debug(DiagnosticWrapper wrapper);

    void warning(DiagnosticWrapper wrapper);

    void error (DiagnosticWrapper wrapper);

    void quiet(String s);
    
    void lifecycle(String s);
    
    void verbose(String s);

    default void info(String message) {
        debug(wrap(message));
    }

    default void info(CharSequence message) {
        debug(wrap(message));
    }
    
    default void info(String format, Object... args) {
        info(String.format(format, args));
    }

    default void debug(String message) {
        debug(wrap(message));
    }

    default void debug(CharSequence message) {
        debug(wrap(message));
    }

    default void warning(String message) {
        DiagnosticWrapper wrapped = wrap(message);
        wrapped.setKind(Diagnostic.Kind.WARNING);
        warning(wrapped);
    }

    default void warning(CharSequence message) {
        DiagnosticWrapper wrapped = wrap(message);
        wrapped.setKind(Diagnostic.Kind.WARNING);
        warning(wrapped);
    }

    default void error(String message) {
       DiagnosticWrapper wrapped = wrap(message);
       wrapped.setKind(Diagnostic.Kind.ERROR);
       error(wrapped);
    }

    default void error(CharSequence message) {
        DiagnosticWrapper wrapped = wrap(message);
        wrapped.setKind(Diagnostic.Kind.ERROR);
        error(wrapped);
    }
    
    default void error(Throwable t, String message) {
        DiagnosticWrapper wrapped = wrap(message + (t != null ? ": " + t.getMessage() : ""));
        wrapped.setKind(Diagnostic.Kind.ERROR);
        error(wrapped);
    }

    static DiagnosticWrapper wrap(CharSequence message) {
        DiagnosticWrapper wrapper = new DiagnosticWrapper();
        wrapper.setMessage(message);
        return wrapper;
    }

    default void lifecycle(String format, Object... args) {
        lifecycle(String.format(format, args));
    }
    
    default void quiet(String format, Object... args) {
        quiet(String.format(format, args));
    }
    
    default void verbose(String format, Object... args) {
        verbose(String.format(format, args));
    }
}
