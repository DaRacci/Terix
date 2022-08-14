package dev.racci.terix.api.origins

import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.extensions.sync
import dev.racci.terix.api.PlayerData
import dev.racci.terix.api.Terix
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.states.State
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration

object OriginHelper : KoinComponent, WithPlugin<Terix> {
    override val plugin by inject<Terix>()

    suspend fun applyBase(
        player: Player,
        origin: Origin = PlayerData.cachedOrigin(player)
    ) {
        State.CONSTANT.activate(player, origin)
        player.setCanBreathUnderwater(origin.waterBreathing)
        player.setImmuneToFire(origin.fireImmunity)
    }

    suspend fun changeTo(
        player: Player,
        oldOrigin: Origin?,
        newOrigin: Origin
    ) {
        player.setImmuneToFire(newOrigin.fireImmunity)
        player.setCanBreathUnderwater(newOrigin.waterBreathing)

        val activeStates = State.getPlayerStates(player)
        val removePotions = arrayListOf<PotionEffectType>()
        val curHealth = player.health

        if (oldOrigin != null) {
            activeStates.forEach { it.deactivate(player, oldOrigin) }
            activeStates.map { getOriginPotions(player, it) }.forEach(removePotions::addAll)
        }

        sync {
            removePotions.forEach(player::removePotionEffect)
            activeStates.forEach { newOrigin.potions[it]?.let(player::addPotionEffects) }
//            when {
//                oldOrigin?.nightVision == true && !newOrigin.nightVision -> {
//                    removePotions += PotionEffectType.NIGHT_VISION
//                }
//                oldOrigin?.nightVision != true && newOrigin.nightVision -> {
//                    val state = activeStates.find { transaction { PlayerData[player].nightVision == it } } ?: return@sync
//                    player.addPotionEffect(nightVisionPotion(newOrigin, state))
//                }
//            }
        }

        activeStates.forEach { it.activate(player, newOrigin) }
        if (player.health < curHealth) player.health = curHealth.coerceAtMost(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value)
    }

    /**
     * If [state] is not null, then returns all potions, which are from the [state].
     * Otherwise, returns all potions that are origin related.
     *
     * @param player The player to get the potions from.
     * @param state The state to get the potions from or null.
     * @return A sequence of the potion effects type.
     */
    fun getOriginPotions(
        player: Player,
        state: State?
    ): Sequence<PotionEffectType> = player.activePotionEffects
        .asSequence()
        .mapNotNull { effect ->
            val match = PotionEffectBuilder.regex.find(effect.key?.asString().orEmpty()) ?: return@mapNotNull null
            if (!(match.groups["type"]?.value == "potion" && state != null && match.groups["state"]?.value == state.name.lowercase())) return@mapNotNull null
            effect.type
        }

    private fun nightVisionPotion(
        origin: Origin,
        trigger: State
    ) = PotionEffectBuilder {
        type = PotionEffectType.NIGHT_VISION
        duration = Duration.INFINITE
        amplifier = 0
        ambient = true
        originKey(origin, trigger)
    }.build()
}
