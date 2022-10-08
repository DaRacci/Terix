package dev.racci.terix.api.dsl

import dev.racci.minix.api.extensions.reflection.accessInvoke
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

inline fun <reified T : CachingBuilder<*>> dslMutator(
    crossinline block: T.() -> Unit
): DSLMutator<T> = object : DSLMutator<T>(T::class) {
    override fun T.mutate() = block()
}

/**
 * Allows easy creation of a DSL for a class.
 *
 * @param T The class to create a DSL for.
 */
abstract class DSLMutator<T : CachingBuilder<*>> @PublishedApi internal constructor(private val kClass: KClass<T>) {

    abstract fun T.mutate()

    /** Mutates this existing instance with the [DSLMutator]. */
    fun on(target: T): T {
        target.mutate()
        return target
    }

    fun mutateOrNew(target: T?): T = if (target != null) on(target) else asNew()

    /**
     * Reflectively creates a new instance of [T] and mutates it with the [DSLMutator].
     *
     * @return A new instance of [T] with the [DSLMutator] applied.
     * @throws IllegalArgumentException If the class does not have a no-args constructor or if the class cannot be found via reflection.
     */
    @Throws(IllegalArgumentException::class)
    fun asNew(): T { //
        val constructor = kClass.primaryConstructor!! // { it.parameters.isEmpty() } ?: error("Cannot find constructor with no parameters for type $kClass.")
        val args = constructor.parameters.map { null }

        return runBlocking { constructor.accessInvoke(*args.toTypedArray()).also { it.mutate() } }
    }
}
