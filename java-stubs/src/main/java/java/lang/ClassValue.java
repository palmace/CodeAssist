package java.lang;

import static java.lang.ClassValue.ClassValueMap.*;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class ClassValue<T> {

    private static final Map<Class<?>, ClassValueMap> MAP_CACHE = new WeakHashMap<>();

    protected abstract T computeValue(Class<?> type);

    public T get(Class<?> type) {
        Entry<?>[] cache;
        Entry<T> e = ClassValueMap.probeHomeLocation(cache = getCacheCarefully(type), this);
        if (match(e)) {
            return e.value();
        }
        return getFromBackup(cache, type);
    }

    private static final Entry<?>[] EMPTY_CACHE = { null };

    private T getFromBackup(Entry<?>[] cache, Class<?> type) {
        Entry<T> e = ClassValueMap.probeBackupLocations(cache, this);
        if (e != null) {
            return e.value();
        }
        return getFromHashMap(type);
    }

    private static Entry<?>[] getCacheCarefully(Class<?> type) {
        ClassValueMap map = getMap(type);
        if (map == null) {
            return EMPTY_CACHE;
        }
        return map.getCache();
    }

    @SuppressWarnings("unchecked")
    Entry<T> castEntry(Entry<?> e) { return (Entry<T>) e; }

    private T getFromHashMap(Class<?> type) {
        ClassValueMap map = getMap(type);
        for (;;) {
            Entry<T> e = map.startEntry(this);
            if (!e.isPromise()) {
                return e.value();
            }
            try {
                e = makeEntry(e.version(), computeValue(type));
            } finally {
                e = map.finishEntry(this, e);
            }
            if (e != null) {
                return e.value();
            }
        }
    }

    boolean match(Entry<?> e) {
        return (e != null && e.get() == this.version);
    }

    final int hashCodeForCache = nextHashCode.getAndAdd(HASH_INCREMENT) & HASH_MASK;
    private static final AtomicInteger nextHashCode = new AtomicInteger();
    private static final int HASH_INCREMENT = 0x61c88647;
    static final int HASH_MASK = (-1 >>> 2);

    static class Identity {
    }
    final Identity identity = new Identity();

    private static ClassValueMap getMap(Class<?> type) {
        ClassValueMap map = MAP_CACHE.get(type);
        if (map != null) {
            return map;
        }
        return initializeMap(type);
    }

    private static final Object CRITICAL_SECTION = new Object();
    private static ClassValueMap initializeMap(Class<?> type) {
        ClassValueMap map;
        synchronized (CRITICAL_SECTION) {
            if ((map = MAP_CACHE.get(type)) == null) {
                map = new ClassValueMap(type);
                MAP_CACHE.put(type, map);
            }
        }
        return map;
    }

    static <T> Entry<T> makeEntry(Version<T> explicitVersion, T value) {
        return new Entry<>(explicitVersion, value);
    }

    private volatile Version<T> version = new Version<>(this);
    Version<T> version() { return version; }
    void bumpVersion() { version = new Version<>(this); }

    static class Version<T> {
        private final ClassValue<T> classValue;
        private final Entry<T> promise = new Entry<>(this);
        Version(ClassValue<T> classValue) { this.classValue = classValue; }
        ClassValue<T> classValue() { return classValue; }
        Entry<T> promise() { return promise; }
        boolean isLive() { return classValue.version() == this; }
    }

    static class Entry<T> extends WeakReference<Version<T>> {
        final Object value;
        Entry(Version<T> version, T value) {
            super(version);
            this.value = value;
        }
        private void assertNotPromise() { assert(!isPromise()); }
        Entry(Version<T> version) {
            super(version);
            this.value = this;
        }
        @SuppressWarnings("unchecked")
        T value() { assertNotPromise(); return (T) value; }
        boolean isPromise() { return value == this; }
        Version<T> version() { return get(); }
        ClassValue<T> classValueOrNull() {
            Version<T> v = version();
            return (v == null) ? null : v.classValue();
        }
        boolean isLive() {
            Version<T> v = version();
            if (v == null) return false;
            if (v.isLive()) return true;
            clear();
            return false;
        }
        Entry<T> refreshVersion(Version<T> v2) {
            assertNotPromise();
            @SuppressWarnings("unchecked")
            Entry<T> e2 = new Entry<>(v2, (T) value);
            clear();
            return e2;
        }
        static final Entry<?> DEAD_ENTRY = new Entry<>(null, null);
    }

    static class ClassValueMap extends WeakHashMap<ClassValue.Identity, ClassValue.Entry<?>> {
        private final Class<?> type;
        private ClassValue.Entry<?>[] cacheArray;
        private int cacheLoad, cacheLoadLimit;
        private static final int INITIAL_ENTRIES = 32;

        ClassValueMap(Class<?> type) {
            this.type = type;
            sizeCache(INITIAL_ENTRIES);
        }

        ClassValue.Entry<?>[] getCache() { return cacheArray; }

        synchronized <T> ClassValue.Entry<T> startEntry(ClassValue<T> classValue) {
            @SuppressWarnings("unchecked")
            ClassValue.Entry<T> e = (ClassValue.Entry<T>) get(classValue.identity);
            Version<T> v = classValue.version();
            if (e == null) {
                e = v.promise();
                put(classValue.identity, e);
                return e;
            } else if (e.isPromise()) {
                if (e.version() != v) {
                    e = v.promise();
                    put(classValue.identity, e);
                }
                return e;
            } else {
                if (e.version() != v) {
                    e = e.refreshVersion(v);
                    put(classValue.identity, e);
                }
                checkCacheLoad();
                addToCache(classValue, e);
                return e;
            }
        }

        synchronized <T> ClassValue.Entry<T> finishEntry(ClassValue<T> classValue, ClassValue.Entry<T> e) {
            @SuppressWarnings("unchecked")
            ClassValue.Entry<T> e0 = (ClassValue.Entry<T>) get(classValue.identity);
            if (e == e0) {
                assert(e.isPromise());
                remove(classValue.identity);
                return null;
            } else if (e0 != null && e0.isPromise() && e0.version() == e.version()) {
                Version<T> v = classValue.version();
                if (e.version() != v) {
                    e = e.refreshVersion(v);
                }
                put(classValue.identity, e);
                checkCacheLoad();
                addToCache(classValue, e);
                return e;
            } else {
                return null;
            }
        }

        static ClassValue.Entry<?> loadFromCache(ClassValue.Entry<?>[] cache, int i) {
            return cache[i & (cache.length-1)];
        }

        static <T> ClassValue.Entry<T> probeHomeLocation(ClassValue.Entry<?>[] cache, ClassValue<T> classValue) {
            return classValue.castEntry(loadFromCache(cache, classValue.hashCodeForCache));
        }

        static <T> ClassValue.Entry<T> probeBackupLocations(ClassValue.Entry<?>[] cache, ClassValue<T> classValue) {
            int mask = (cache.length-1);
            int home = (classValue.hashCodeForCache & mask);
            ClassValue.Entry<?> e2 = cache[home];
            if (e2 == null) return null;
            int pos2 = -1;
            for (int i = home + 1; i < home + 6; i++) {
                ClassValue.Entry<?> e = cache[i & mask];
                if (e == null) break;
                if (classValue.match(e)) {
                    cache[home] = e;
                    if (pos2 >= 0) {
                        cache[i & mask] = ClassValue.Entry.DEAD_ENTRY;
                    } else {
                        pos2 = i;
                    }
                    cache[pos2 & mask] = ((entryDislocation(cache, pos2, e2) < 6) ? e2 : ClassValue.Entry.DEAD_ENTRY);
                    return classValue.castEntry(e);
                }
                if (!e.isLive() && pos2 < 0) pos2 = i;
            }
            return null;
        }

        private static int entryDislocation(ClassValue.Entry<?>[] cache, int pos, ClassValue.Entry<?> e) {
            ClassValue<?> cv = e.classValueOrNull();
            if (cv == null) return 0;
            int mask = (cache.length-1);
            return (pos - cv.hashCodeForCache) & mask;
        }

        private void sizeCache(int length) {
            cacheLoad = 0;
            cacheLoadLimit = (int) ((double) length * 67 / 100);
            cacheArray = new ClassValue.Entry<?>[length];
        }

        private void checkCacheLoad() {
            if (cacheLoad >= cacheLoadLimit) reduceCacheLoad();
        }

        private void reduceCacheLoad() {
            removeStaleEntries();
            if (cacheLoad < cacheLoadLimit) return;
            ClassValue.Entry<?>[] oldCache = getCache();
            sizeCache(oldCache.length * 2);
            for (ClassValue.Entry<?> e : oldCache) {
                if (e != null && e.isLive()) addToCache(e);
            }
        }

        private void removeStaleEntries() {
            ClassValue.Entry[] cache = getCache();
            for (int i = 0; i < cache.length; i++) {
                ClassValue.Entry<?> e = cache[i];
                if (e != null && !e.isLive()) cache[i] = null;
            }
        }

        private void removeStaleEntries(ClassValue<?> classValue) {
            removeStaleEntries();
        }

        private <T> void addToCache(ClassValue.Entry<T> e) {
            ClassValue<T> classValue = e.classValueOrNull();
            if (classValue != null) addToCache(classValue, e);
        }

        private <T> void addToCache(ClassValue<T> classValue, ClassValue.Entry<T> e) {
            ClassValue.Entry<?>[] cache = getCache();
            int mask = (cache.length-1);
            int home = classValue.hashCodeForCache & mask;
            ClassValue.Entry<?> e2 = placeInCache(cache, home, e, false);
            if (e2 == null) return;
            int dis2 = entryDislocation(cache, home, e2);
            int home2 = home - dis2;
            for (int i2 = home2; i2 < home2 + 6; i2++) {
                if (placeInCache(cache, i2 & mask, e2, true) == null) return;
            }
        }

        private ClassValue.Entry<?> placeInCache(ClassValue.Entry<?>[] cache, int pos, ClassValue.Entry<?> e, boolean gently) {
            ClassValue.Entry<?> e2 = overwrittenEntry(cache[pos]);
            if (gently && e2 != null) return e;
            cache[pos] = e;
            return e2;
        }

        private <T> ClassValue.Entry<T> overwrittenEntry(ClassValue.Entry<T> e2) {
            if (e2 == null) {
                cacheLoad += 1;
            } else if (e2.isLive()) {
                return e2;
            }
            return null;
        }
    }
}