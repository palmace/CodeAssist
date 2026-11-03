package com.tyron.builder.project.impl;

import androidx.annotation.Nullable;
import com.tyron.builder.model.ModuleSettings;
import com.tyron.builder.project.api.FileManager;
import com.tyron.builder.project.api.Module;
import com.tyron.builder.project.cache.CacheHolder.CacheKey;
import com.tyron.common.util.Cache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.com.intellij.openapi.util.Key;
import org.jetbrains.kotlin.com.intellij.openapi.util.KeyWithDefaultValue;
import org.jetbrains.kotlin.com.intellij.util.keyFMap.KeyFMap;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * ModuleImpl v44.1.20: "Zenith Core - Atomic Foundation".
 * v44.1.20: Zenith Core - Optimización de sincronía estructural y propagación instantánea de cambios.
 * v44.1.20: Concurrent Map para cache - Reducción de latencia en el Puente Quantum.
 * v35.3: Corregido - Se evita getCanonicalFile() que rompe el acceso en Android 15 (/data/media vs /storage).
 * v35.3: Anclaje Absoluto via getAbsoluteFile() para estabilidad post-crash.
 */
public class ModuleImpl implements Module {

    @NotNull
    private volatile KeyFMap myUserMap = KeyFMap.EMPTY_MAP;
    private final File mRoot;
    private volatile ModuleSettings myModuleSettings;
    private final FileManager mFileManager;
    private final Map<CacheKey<?, ?>, Cache<?, ?>> mCacheMap = new ConcurrentHashMap<>();

    public ModuleImpl(File root) {
        // v44.1.20: Refuerzo de anclaje absoluto
        this.mRoot = (root != null) ? root.getAbsoluteFile() : new File(".").getAbsoluteFile();
        this.mFileManager = new FileManagerImpl(mRoot);
    }

    @Override
    public void open() throws IOException {
        // v44.1.20: Carga atómica de configuración
        myModuleSettings = new ModuleSettings(new File(getRootFile(), "app_config.json"));
    }

    @Override
    public File getBuildDirectory() {
        return new File(getRootFile(), "build").getAbsoluteFile();
    }

    protected File getPathSetting(String key) {
        String path = getSettings().getString(key, "");
        return new File(path).getAbsoluteFile();
    }

    @Override
    public File getRootFile() { return mRoot; }

    @Override public void clear() {
        mCacheMap.clear();
    }
    
    @Override public void index() {}
    
    @Override public ModuleSettings getSettings() { 
        if (myModuleSettings == null) {
            try { open(); } catch (IOException ignored) {}
        }
        return myModuleSettings; 
    }
    
    @Override public FileManager getFileManager() { return mFileManager; }

    @Nullable
    @Override
    public <T> T getUserData(@NotNull Key<T> key) {
        T t = myUserMap.get(key);
        if (t == null && key instanceof KeyWithDefaultValue) {
            t = ((KeyWithDefaultValue<T>) key).getDefaultValue();
            putUserData(key, t);
        }
        return t;
    }

    @Override
    public synchronized <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
        KeyFMap map = myUserMap;
        myUserMap = value == null ? map.minus(key) : map.plus(key, value);
    }

    @NotNull
    @Override
    public synchronized <T> T putUserDataIfAbsent(@NotNull Key<T> key, @NotNull T value) {
        KeyFMap map = myUserMap;
        T oldValue = map.get(key);
        if (oldValue != null) return oldValue;
        myUserMap = map.plus(key, value);
        return value;
    }

    @Override
    public synchronized <T> boolean replace(@NotNull Key<T> key, @Nullable T oldValue, @Nullable T newValue) {
        KeyFMap map = myUserMap;
        if (map.get(key) != oldValue) return false;
        myUserMap = newValue == null ? map.minus(key) : map.plus(key, newValue);
        return true;
    }

    @Override public int hashCode() { return mRoot.hashCode(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModuleImpl)) return false;
        ModuleImpl project = (ModuleImpl) o;
        return mRoot.equals(project.mRoot);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(CacheKey<K, V> key, Cache<K, V> defaultValue) {
        return (Cache<K, V>) mCacheMap.computeIfAbsent(key, k -> defaultValue);
    }

    @Override public <K, V> void put(CacheKey<K, V> key, Cache<K, V> value) { 
        mCacheMap.put(key, value); 
    }
}
