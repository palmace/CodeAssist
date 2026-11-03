package com.tyron.builder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * SovereignMessage v1.0: "The Ghost Message".
 * Contenedor de datos Java puro para el túnel SovereignSignal.
 * Reemplaza a Intent para evitar errores de compilación en PC.
 */
public class SovereignMessage implements Serializable {
    public final String action;
    private final Map<String, Serializable> extras = new HashMap<>();

    public SovereignMessage(String action) {
        this.action = action;
    }

    public void putExtra(String key, Serializable value) {
        extras.put(key, value);
    }

    public String getStringExtra(String key) {
        Object val = extras.get(key);
        return val instanceof String ? (String) val : null;
    }

    public boolean getBooleanExtra(String key, boolean defaultValue) {
        Object val = extras.get(key);
        return val instanceof Boolean ? (Boolean) val : defaultValue;
    }

    public int getIntExtra(String key, int defaultValue) {
        Object val = extras.get(key);
        return val instanceof Integer ? (Integer) val : defaultValue;
    }

    public Serializable getSerializableExtra(String key) {
        return extras.get(key);
    }
}
