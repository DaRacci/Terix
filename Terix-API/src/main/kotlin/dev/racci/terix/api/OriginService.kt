package dev.racci.terix.api

import dev.racci.minix.api.utils.getKoin
import dev.racci.terix.api.origins.AbstractAbility
import dev.racci.terix.api.origins.AbstractOrigin
import kotlinx.collections.immutable.PersistentMap
import kotlin.reflect.KClass

interface OriginService {

    fun getAbilities(): PersistentMap<KClass<out AbstractAbility>, AbstractAbility>

    fun getOrigins(): PersistentMap<KClass<out AbstractOrigin>, AbstractOrigin>

    fun getOrigin(origin: KClass<out AbstractOrigin>): AbstractOrigin

    fun getOriginOrNull(origin: KClass<out AbstractOrigin>): AbstractOrigin?

    fun getAbility(ability: KClass<out AbstractAbility>): AbstractAbility

    fun getAbilityOrNull(ability: KClass<out AbstractAbility>): AbstractAbility?

    fun getOrigin(name: String): AbstractOrigin

    fun getOriginOrNull(name: String?): AbstractOrigin?

    companion object : OriginService by getKoin().get()
}
