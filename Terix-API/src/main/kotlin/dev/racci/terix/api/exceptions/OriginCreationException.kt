package dev.racci.terix.api.exceptions

class OriginCreationException(
    reason: String,
    cause: Throwable? = null
) : Exception(reason, cause)
