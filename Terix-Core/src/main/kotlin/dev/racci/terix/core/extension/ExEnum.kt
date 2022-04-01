package dev.racci.terix.core.extension

@Deprecated("Use method instead", ReplaceWith("Enum<*>.formatted(seperator, capitalize)"))
val Enum<*>.formatted: String
    get() = name.lowercase().replace("_", " ").replaceFirstChar { it.titlecase() }

fun Enum<*>.formatted(
    separator: String = " ",
    capitalize: Boolean = true
): String {
    val split = name.lowercase().split("_").toMutableList()
    if (capitalize) {
        split.forEachIndexed { index, s ->
            split[index] = s.replaceFirstChar { it.titlecase() }
        }
    }
    return split.joinToString(separator)
}
