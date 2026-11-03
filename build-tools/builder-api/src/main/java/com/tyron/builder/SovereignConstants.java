package com.tyron.builder;

/**
 * SovereignConstants v5.0: "The Ultimate Diamond Oracle".
 * v5.0: Unificación definitiva para el enjambre OMNI.
 * v5.0: Visible por el IDE (:app) y el Motor de Construcción (:build-logic).
 */
public class SovereignConstants {
    
    // ACCIONES DE PETICIÓN
    public static final String ACTION_COMPILE = "com.tyron.code.ACTION_COMPILE";
    public static final String ACTION_DIAGNOSTIC = "com.tyron.code.ACTION_DIAGNOSTIC";
    public static final String ACTION_COMPLETE = "com.tyron.code.ACTION_COMPLETE";
    public static final String ACTION_HEARTBEAT = "com.tyron.code.HEARTBEAT";
    
    // RESULTADOS (TÚNEL SOBERANO)
    public static final String ACTION_COMPILE_RESULT = "com.tyron.code.COMPILE_RESULT";
    public static final String ACTION_JAVA_DIAGNOSTIC_RESULT = "com.tyron.code.JAVA_DIAGNOSTIC_RESULT";
    public static final String ACTION_KOTLIN_DIAGNOSTIC_RESULT = "com.tyron.code.DIAGNOSTIC_RESULT";
    public static final String ACTION_JAVA_COMPLETION_RESULT = "com.tyron.code.JAVA_COMPLETION_RESULT";
    public static final String ACTION_DEX_RESULT = "com.tyron.code.DEX_RESULT";

    // LLAVES DE DATOS (EXTRAS)
    public static final String EXTRA_FILE_PATH = "file_path";
    public static final String EXTRA_CONTENT = "content";
    public static final String EXTRA_CLASSPATH = "classpath";
    public static final String EXTRA_BOOT_CLASSPATH = "boot_classpath";
    public static final String EXTRA_CURSOR_OFFSET = "cursor_offset";
    public static final String EXTRA_REQUEST_ID = "request_id";
    public static final String EXTRA_SUCCESS = "success";
    public static final String EXTRA_OUTPUT = "output";
    public static final String EXTRA_ERROR = "error";
    public static final String EXTRA_DIAGNOSTICS = "diagnostics";
    public static final String EXTRA_COMPLETION_LIST = "completion_list";
}
