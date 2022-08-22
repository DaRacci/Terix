package dev.racci.terix.api.origins

import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.extensions.sync
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.PlayerData
import dev.racci.terix.api.Terix
import dev.racci.terix.api.dsl.AttributeModifierBuilder
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.states.State
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object OriginHelper : KoinComponent, WithPlugin<Terix> {
    private const val SCOPE = "terix.origin.helper"
    override val plugin by inject<Terix>()

    /** Checks if a player should be ignored for something like staff mode. */
    fun shouldIgnorePlayer(player: Player): Boolean {
        // TODO -> Vanished
        // TODO -> Partial Ignore
        // TODO -> Staff Mode
        fun ignoring() = when {
            player.gameMode.ordinal !in 1..2 -> IgnoreReason.GAMEMODE
            else -> null
        }

        val reason = ignoring()
        if (reason != null) plugin.log.trace(scope = SCOPE) { "Ignoring player ${player.name}: ${reason.description}" }

        return reason != null
    }

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

    /** Increases the players' health safely by clamping it to the maximum health. */
    fun increaseHealth(
        player: Player,
        amount: Double
    ) {
        val curHealth = player.health
        val maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value
        val newHealth = curHealth + amount
        player.health = newHealth.coerceAtMost(maxHealth)
    }

    /** Designed to be invoked for when a player needs everything to reset. */
    suspend fun activateOrigin(player: Player) {
        val origin = PlayerData.cachedOrigin(player)

        player.setImmuneToFire(origin.fireImmunity)
        player.setCanBreathUnderwater(origin.waterBreathing)

        State.recalculateAllStates(player)
        State.getPlayerStates(player).forEach { it.activate(player, origin) }
    }

    /** Designed to be invoked for when a player needs everything disabled. */
    suspend fun deactivateOrigin(player: Player) {
        PlayerData.cachedOrigin(player).abilities.values.forEach { it.deactivate(player) }
        getOriginPotions(player, null).forEach(player::removePotionEffect)
        State.activeStates.remove(player)
        player.setImmuneToFire(null)
        player.setCanBreathUnderwater(null)

        for (attribute in Attribute.values()) {
            val instance = player.getAttribute(attribute) ?: continue
            if (instance.modifiers.isEmpty()) continue

            instance.modifiers.associateWith { AttributeModifierBuilder.regex.matchEntire(it.name) }
                .forEach { (modifier, match) ->
                    if (match == null) return@forEach
                    instance.removeModifier(modifier)
                }
        }
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

    fun potionState(potion: PotionEffect): State? {
        val match = PotionEffectBuilder.regex.find(potion.key?.asString().orEmpty()) ?: return null
        return State.values.find { it.name.lowercase() == match.groups["state"]?.value }
    }

    fun potionOrigin(potion: PotionEffect): Origin? {
        val match = PotionEffectBuilder.regex.find(potion.key?.asString().orEmpty()) ?: return null
        return OriginService.getOriginOrNull(match.groups["from"]?.value)
    }

    enum class IgnoreReason(val description: String) { GAMEMODE("Not in survival or adventure mode") }
}
