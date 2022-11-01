package dev.racci.terix.api.exceptions

public class OriginCreationException(
    reason: String,
    cause: Throwable? = null
) : Exception(reason, cause)
