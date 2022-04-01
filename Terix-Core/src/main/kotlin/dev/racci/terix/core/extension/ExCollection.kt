package dev.racci.terix.core.extension

@Suppress("unchecked_cast") // This is a safe cast util
fun <C> Array<*>.getCast(index: Int) = getOrNull(index) as? C
