package dev.racci.terix.api.dsl

import dev.racci.minix.api.extensions.reflection.accessWith
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.minix.api.extensions.reflection.safeCast
import kotlinx.coroutines.runBlocking
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1

public abstract class CachingBuilder<T> {
    @[Transient PublishedApi] internal var cached: T? = null

    @[Transient PublishedApi] internal var dirty: Boolean = true

    @Transient protected val watcherSet: MutableSet<ChangeWatcher<*>> = mutableSetOf()

    @Transient protected val watcherValues: List<Any?> = watcherSet.map(ChangeWatcher<*>::property)
        .filterIsInstance<KProperty1<CachingBuilder<*>, *>>()
        .map { it.get(this) }

    /** Creates a new [T] instance from the current state of this builder. */
    protected abstract fun create(): T

    /** Returns the cached [T] instance, or creates a new one if it is dirty. */
    public fun get(): T {
        if (dirty) {
            cached = create()
            dirty = false
        }

        return cached!!
    }

    /** Creates a property to be watched and mark this builder as dirty when changed. */
    protected fun <R : Any> createWatcher(initValue: R?): PropertyDelegateProvider<CachingBuilder<*>, ChangeWatcher<R>> = PropertyDelegateProvider { thisRef, property -> ChangeWatcher(thisRef, property, initValue) }

    protected fun <R> KProperty0<R>.watcherOrNull(): R? {
        return runBlocking {
            this@watcherOrNull.accessWith {
                (this.getDelegate() as ChangeWatcher<*>).value.safeCast()
            }
        }
    }

    final override fun toString(): String = buildString {
        append("CachingBuilder(")
        append("type=").append(this@CachingBuilder::class.simpleName)
        append(", cached=").append(cached)
        append(", dirty=").append(dirty)

        if (watcherSet.isNotEmpty()) {
            append(", watchers=[")

            watcherSet.forEachIndexed { index, watcher ->
                if (index != 0) append(", ")

                append(watcher.property.name)
                append("=")
                append(watcher.value)
            }

            append("]")
        }
    }

    final override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (this === other) return true
        if (other !is CachingBuilder<*>) return false

        if (cached != other.cached) return false
        if (dirty != other.dirty) return false

        return watcherValues.withIndex().all { (index, value) -> value == other.watcherValues[index] }
    }

    final override fun hashCode(): Int {
        var result = cached?.hashCode() ?: 0

        result = 31 * result + dirty.hashCode()
        watcherValues.forEach { result = 31 * result + it.hashCode() }

        return result
    }

    protected class ChangeWatcher<R : Any>(
        private val thisRef: CachingBuilder<*>,
        public val property: KProperty<*>,
        initialValue: R?
    ) : ReadWriteProperty<Any?, R> {
        internal var value = initialValue

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: R) {
            val oldValue = this.value
            this.value = value
            if (oldValue != value) this.thisRef.dirty = true
        }

        override fun getValue(thisRef: Any?, property: KProperty<*>): R {
            return value ?: throw IllegalStateException("Property ${property.name} should be initialized before get.")
        }

        init {
            thisRef.watcherSet.add(this.castOrThrow())
        }
    }
}
