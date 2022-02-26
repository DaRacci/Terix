package dev.racci.terix.core.extension

val Enum<*>.formatted: String
    get() = name.lowercase().replace("_", " ").replaceFirstChar { it.titlecase() }
