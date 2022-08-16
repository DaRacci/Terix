package dev.racci.terix.api

import dev.racci.minix.api.utils.getKoin
import dev.racci.terix.api.origins.abilities.Ability
import dev.racci.terix.api.origins.origin.Origin
import kotlinx.collections.immutable.PersistentMap
import kotlin.reflect.KClass

interface OriginService {

    val defaultOrigin: Origin

    fun getAbilities(): PersistentMap<KClass<out Ability>, Ability>

    fun getOrigins(): PersistentMap<KClass<out Origin>, Origin>

    fun getOrigin(origin: KClass<out Origin>): Origin

    fun getOriginOrNull(origin: KClass<out Origin>): Origin?

    fun getAbility(ability: KClass<out Ability>): Ability

    fun getAbilityOrNull(ability: KClass<out Ability>): Ability?

    fun getOrigin(name: String): Origin

    fun getOriginOrNull(name: String?): Origin?

    companion object : OriginService by getKoin().get()
}
