package dev.racci.terix.api.origins

import arrow.core.toOption
import dev.racci.minix.api.coroutine.asyncDispatcher
import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.terix.api.Terix
import dev.racci.terix.api.data.OriginNamespacedTag
import dev.racci.terix.api.data.player.TerixPlayer
import dev.racci.terix.api.extensions.originPotions
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.states.State
import dev.racci.terix.api.sentryScoped
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

public object OriginHelper : KoinComponent, WithPlugin<Terix> {
    override val plugin: Terix by inject()

    /** Checks if a player should be ignored for something like staff mode. */
    public fun shouldIgnorePlayer(player: Player): Boolean {
        // TODO -> Vanished
        // TODO -> Partial Ignore
        // TODO -> Staff Mode
        fun ignoring() = when {
            player.gameMode.ordinal !in 1..2 -> IgnoreReason.GAMEMODE
            else -> null
        }

        val reason = ignoring()

        return reason != null
    }

    public suspend fun changeTo(
        player: Player,
        oldOrigin: Origin?,
        newOrigin: Origin
    ): Unit = sentryScoped(player, "OriginHelper.changeTo", "Changing from ${oldOrigin?.name} to ${newOrigin.name}", context = plugin.asyncDispatcher) {
        if (oldOrigin != null) {
            oldOrigin.handleChangeOrigin(player)
            this.deactivateOrigin(player, oldOrigin)
        }

        this.activateOrigin(player, newOrigin)
        this.setHealth(player, player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value)
    }

    /**
     * Increases the players' health safely by clamping it within 0 and the maximum health.
     *
     * @param player The player to increase the health of.
     * @param amount The amount to increase the health by.
     */
    public fun increaseHealth(
        player: Player,
        amount: Double
    ) {
        val newHealth = player.health + amount
        this.setHealth(player, newHealth)
    }

    /**
     * Set the players' health safely by clamping it within 0 and the maximum health.
     *
     * @param player The player to set the health of.
     * @param amount The amount to set the health to.
     */
    public fun setHealth(
        player: Player,
        amount: Double
    ) {
        val maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value
        player.health = amount.coerceIn(0.0, maxHealth)
    }

    /** Designed to be invoked for when a player needs everything to reset. */
    public suspend fun activateOrigin(
        player: Player,
        origin: Origin = TerixPlayer.cachedOrigin(player)
    ) {
        sentryScoped(player, "OriginHelper.activateOrigin", "Activating ${origin.name} for ${player.name}") {
            player.setCanBreathUnderwater(origin.waterBreathing)
            player.setImmuneToFire(origin.fireImmunity)

            this.recalculateStates(player, origin)

            origin.handleBecomeOrigin(player)
            origin.abilityData.create(player)
        }
    }

    /**
     * Completely disables all parts of the players' origin.
     *
     * @param player The player to disable the origin for.
     * @param origin The origin to disable.
     */
    public suspend fun deactivateOrigin(
        player: Player,
        origin: Origin = TerixPlayer[player].origin
    ) {
        sentryScoped(
            player,
            "OriginHelper.deactivateOrigin",
            "Deactivating ${origin.name} for ${player.name}"
        ) {
            origin.handleDeactivate(player)
            player.setImmuneToFire(null)
            player.setCanBreathUnderwater(null)
            State.activeStates[player].forEach { state -> state.deactivate(player, origin) }
            origin.abilityData.close(player)

//            sync { getBaseOriginPotions(player, null).forEach(player::removePotionEffect) }

//            for (attribute in Attribute.values()) {
//                val instance = player.getAttribute(attribute) ?: continue
//                if (instance.modifiers.isEmpty()) continue
//
//                instance.modifiers.associateWith { OriginNamespacedTag.REGEX.matchEntire(it.name) }
//                    .forEach { (modifier, match) ->
//                        if (match == null) return@forEach
//                        instance.removeModifier(modifier)
//                    }
//            }
        }
    }

    public suspend fun recalculateStates(
        player: Player,
        origin: Origin = TerixPlayer.cachedOrigin(player)
    ) {
        State.activeStates[player].forEach { state ->
            state.deactivate(player, origin)
        }

        State.values.asSequence()
            .filterIsInstance<State.StatedSource<*>>()
            .filter { state -> state[player] }
            .forEach { state -> state.activate(player, origin) }
    }

    /**
     * If [state] is not null, then returns all potions, which are from the [state].
     * Otherwise, returns all potions that are origin related.
     *
     * @param player The player to get the potions from.
     * @param state The state to get the potions from or null.
     * @return A sequence of the potion effects type.
     */
    // TODO -> Merge into respective sources (State, Food, etc.)
    public fun getBaseOriginPotions(
        player: Player,
        state: State?
    ): Sequence<PotionEffectType> = player.originPotions(false)
        .filter { (_, tag) -> tag.isCauseTypeState }
        .filter { (_, tag) -> state == null || tag.fromState(state) }
        .map { (potion, _) -> potion.type }

    public fun potionState(potion: PotionEffect): State? {
        return OriginNamespacedTag.fromBukkitKey(potion.key).toOption()
            .filter { it.isCauseTypeState }
            .map { it.getState() }.orNull()
    }

    public fun potionOrigin(potion: PotionEffect): Origin? {
        return OriginNamespacedTag.fromBukkitKey(potion.key).toOption().map { it.getOrigin() }.orNull()
    }

    public enum class IgnoreReason(public val description: String) { GAMEMODE("Not in survival or adventure mode") }
}
