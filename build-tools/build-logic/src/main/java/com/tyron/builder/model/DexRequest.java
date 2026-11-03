package com.tyron.builder.model;

import java.io.Serializable;
import java.util.List;

/**
 * Modelo de datos v39.0: "Structural DEX IPC".
 * Sincronizado para el ensamble atómico de Motorider.
 */
public class DexRequest implements Serializable {
    private static final long serialVersionUID = 4L;

    public List<String> inputs;
    public String outputDir;
    public int minApi;
    public boolean isRelease;
    public List<String> libraryFiles;

    public DexRequest() {}
}
