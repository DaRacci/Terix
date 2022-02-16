package dev.racci.terix.core.extension

fun <C> Array<*>.getCast(index: Int) = getOrNull(index) as? C
