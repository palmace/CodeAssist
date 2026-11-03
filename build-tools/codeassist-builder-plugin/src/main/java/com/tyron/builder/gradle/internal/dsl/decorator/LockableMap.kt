package com.tyron.builder.gradle.internal.dsl.decorator

import com.tyron.builder.gradle.internal.dsl.AgpDslLockedException
import com.tyron.builder.gradle.internal.dsl.Lockable
import java.util.AbstractMap
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Function
import javax.inject.Inject

/**
 * LockableMap suturado para compatibilidad total con Java 17 y Kotlin 1.9.
 */
class LockableMap<K, V> @Inject constructor(
    private val name: String
) : AbstractMap<K, V>(), MutableMap<K, V>, Lockable {

    private val delegate: MutableMap<K, V> = mutableMapOf()
    private var lockableEntrySet: LockableSet<MutableMap.MutableEntry<K, V>>? = null
    private var locked = false

    override fun lock() {
        locked = true
        lockableEntrySet?.lock()
    }

    private inline fun <R> check(action: () -> R): R {
        if (locked) {
            throw AgpDslLockedException("It is too late to modify ${name.removePrefix("_")}")
        }
        return action.invoke()
    }

    override fun put(key: K, value: V): V? = check { delegate.put(key, value) }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() {
            if (lockableEntrySet == null) {
                lockableEntrySet = LockableSet("${name}_entrySet", delegate.entries).also {
                    if (locked) it.lock()
                }
            }
            return lockableEntrySet!!
        }

    override fun getOrDefault(key: K, defaultValue: @UnsafeVariance V): V = delegate.getOrDefault(key, defaultValue)
    override fun remove(key: K, value: @UnsafeVariance V): Boolean = check { delegate.remove(key, value) }
    
    // Ajuste de firmas para evitar errores de 'overrides nothing' en Kotlin 1.9/JDK 17
    @Suppress("UNCHECKED_CAST")
    override fun computeIfPresent(key: K, remappingFunction: BiFunction<in K, in V & Any, out V?>): V? = check {
        delegate.computeIfPresent(key, remappingFunction as BiFunction<in K, in V, out V?>)
    }

    @Suppress("UNCHECKED_CAST")
    override fun compute(key: K, remappingFunction: BiFunction<in K, in V?, out V?>): V? = check {
        delegate.compute(key, remappingFunction as BiFunction<in K, in V?, out V?>)
    }

    @Suppress("UNCHECKED_CAST")
    override fun merge(key: K, value: V & Any, remappingFunction: BiFunction<in V & Any, in V & Any, out V?>): V? = check {
        delegate.merge(key, value, remappingFunction as BiFunction<in V, in V, out V?>)
    }

    override fun forEach(action: BiConsumer<in K, in V>) = delegate.forEach(action)
    override fun replaceAll(function: BiFunction<in K, in V, out V>) = check { delegate.replaceAll(function) }
    override fun putIfAbsent(key: K, value: V): V? = check { delegate.putIfAbsent(key, value) }
    override fun replace(key: K, oldValue: @UnsafeVariance V, newValue: V): Boolean = check { delegate.replace(key, oldValue, newValue) }
    override fun replace(key: K, value: V): V? = check { delegate.replace(key, value) }
    
    @Suppress("UNCHECKED_CAST")
    override fun computeIfAbsent(key: K, mappingFunction: Function<in K, out V>): V = check { 
        delegate.computeIfAbsent(key, mappingFunction as Function<in K, out V>)
    }
}
