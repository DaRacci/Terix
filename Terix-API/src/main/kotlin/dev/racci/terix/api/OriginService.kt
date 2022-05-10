package dev.racci.terix.api

import dev.racci.minix.api.extension.Extension
import dev.racci.terix.api.origins.AbstractAbility
import dev.racci.terix.api.origins.AbstractOrigin
import kotlinx.collections.immutable.PersistentMap
import kotlin.reflect.KClass

abstract class OriginService : Extension<Terix>() {

    abstract fun getAbilities(): PersistentMap<KClass<out AbstractAbility>, AbstractAbility>

    abstract fun getOrigins(): PersistentMap<KClass<out AbstractOrigin>, AbstractOrigin>

    abstract fun getOrigin(origin: KClass<out AbstractOrigin>): AbstractOrigin

    abstract fun getOriginOrNull(origin: KClass<out AbstractOrigin>): AbstractOrigin?

    abstract fun getAbility(ability: KClass<out AbstractAbility>): AbstractAbility

    abstract fun getAbilityOrNull(ability: KClass<out AbstractAbility>): AbstractAbility?

    abstract fun getOrigin(name: String): AbstractOrigin

    abstract fun getOriginOrNull(name: String): AbstractOrigin?

    companion object : ExtensionCompanion<OriginService>()
}
