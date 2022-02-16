package dev.racci.terix.core.extension

fun Boolean?.asInt(): Int = if (this == true) 1 else 0
