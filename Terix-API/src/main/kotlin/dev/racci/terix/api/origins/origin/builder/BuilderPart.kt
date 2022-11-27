package dev.racci.terix.api.origins.origin.builder

import arrow.core.Option
import dev.racci.terix.api.origins.origin.OriginValues
import org.apiguardian.api.API

public sealed class BuilderPart<T : Any> {
    private val heldElements: MutableList<T> = mutableListOf()

    @PublishedApi internal fun addElement(element: T) {
        heldElements.add(element)
    }

    protected fun getElements(): List<T> = heldElements.toList()

    @API(status = API.Status.INTERNAL)
    public abstract suspend fun insertInto(originValues: OriginValues): Option<Exception>
}
