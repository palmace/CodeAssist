package com.tyron.builder.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de datos v44.0: "Zenith Total Link".
 * v44.0: Soporte para argumentos libres (extraArgs) para control del compilador.
 * v44.0: Flag para ignorar chequeo de versión de metadatos.
 */
public class CompileRequest implements Serializable {
    private static final long serialVersionUID = 7L;

    public static final String ACTION_COMPILE_RESULT = "com.tyron.code.COMPILE_RESULT";
    public static final String EXTRA_SUCCESS = "success";
    public static final String EXTRA_ERROR = "error";

    public String projectRoot;
    public List<String> sourceFiles;
    public List<String> classpath;
    public List<String> sourcePath;
    public List<String> processorPath;
    public List<String> processors;
    public String outputDir;
    public String androidJarPath;
    
    public List<String> extraArgs = new ArrayList<>();
    
    public boolean allowImplicit = false;
    public boolean skipMetadataVersionCheck = true;
    
    public CompileRequest() {}
}
