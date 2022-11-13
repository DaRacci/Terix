package dev.racci.terix.api

import dev.racci.minix.api.utils.getKoin
import dev.racci.terix.api.origins.origin.Origin
import kotlinx.collections.immutable.PersistentMap
import kotlin.reflect.KClass

public interface OriginService {

    public val defaultOrigin: Origin

    public fun getOrigins(): PersistentMap<KClass<out Origin>, Origin>

    @Throws(NoSuchElementException::class)
    public fun getOrigin(origin: KClass<out Origin>): Origin

    @Throws(NoSuchElementException::class)
    public fun getOrigin(name: String): Origin

    public fun getOriginOrNull(name: String?): Origin?

    public companion object : OriginService by getKoin().get()
}
