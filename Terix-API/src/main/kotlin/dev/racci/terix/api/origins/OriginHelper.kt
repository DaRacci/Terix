package dev.racci.terix.api.origins

import arrow.core.toOption
import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.extensions.collections.clear
import dev.racci.terix.api.Terix
import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.TerixPlayer.User.origin
import dev.racci.terix.api.data.OriginNamespacedTag
import dev.racci.terix.api.extensions.originPotions
import dev.racci.terix.api.origins.abilities.Ability
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

    public suspend fun applyBase(
        player: Player,
        origin: Origin = TerixPlayer.cachedOrigin(player)
    ) {
        State.CONSTANT.activate(player, origin)
        player.setCanBreathUnderwater(origin.waterBreathing)
        player.setImmuneToFire(origin.fireImmunity)
    }

    public suspend fun changeTo(
        player: Player,
        oldOrigin: Origin?,
        newOrigin: Origin
    ): Unit = sentryScoped(player, "OriginHelper.changeTo", "Changing from ${oldOrigin?.name} to ${newOrigin.name}") {
        player.setImmuneToFire(newOrigin.fireImmunity)
        player.setCanBreathUnderwater(newOrigin.waterBreathing)

        val activeStates = State.getPlayerStates(player)
        val removePotions = arrayListOf<PotionEffectType>()
        val curHealth = player.health

        if (oldOrigin != null) {
            oldOrigin.handleChangeOrigin(player)
            activeStates.forEach { it.deactivate(player, oldOrigin) }
            activeStates.map { getBaseOriginPotions(player, it) }.forEach(removePotions::addAll)
            unregisterAbilities(oldOrigin, player)
        }

        sync {
            removePotions.forEach(player::removePotionEffect)
            activeStates.forEach { newOrigin.statePotions[it]?.let(player::addPotionEffects) }
        }

        newOrigin.handleBecomeOrigin(player)
        registerAbilities(newOrigin, player)
        activeStates.forEach { it.activate(player, newOrigin) }
        if (player.health < curHealth) player.health =
            curHealth.coerceAtMost(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value)
    }

    /** Increases the players' health safely by clamping it to the maximum health. */
    public fun increaseHealth(
        player: Player,
        amount: Double
    ) {
        val curHealth = player.health
        val maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value
        val newHealth = curHealth + amount
        player.health = newHealth.coerceAtMost(maxHealth)
    }

    /** Designed to be invoked for when a player needs everything to reset. */
    public suspend fun activateOrigin(
        player: Player,
        origin: Origin = TerixPlayer.cachedOrigin(player)
    ) {
        sentryScoped(player, "OriginHelper.activateOrigin", "Activating ${origin.name} for ${player.name}") {
            applyBase(player, origin)

            State.recalculateAllStates(player)
            State.getPlayerStates(player).forEach { it.activate(player, origin) }
            registerAbilities(origin, player)
        }
    }

    /** Designed to be invoked for when a player needs everything disabled. */
    public suspend fun deactivateOrigin(player: Player) {
        sentryScoped(player, "OriginHelper.deactivateOrigin", "Deactivating ${origin.name} for ${player.name}") {
            sync { getBaseOriginPotions(player, null).forEach(player::removePotionEffect) }

            async {
                val origin = TerixPlayer.cachedOrigin(player)
                origin.handleDeactivate(player)
                State.activeStates.remove(player)
                player.setImmuneToFire(null)
                player.setCanBreathUnderwater(null)
                unregisterAbilities(origin, player)

                for (attribute in Attribute.values()) {
                    val instance = player.getAttribute(attribute) ?: continue
                    if (instance.modifiers.isEmpty()) continue

                    instance.modifiers.associateWith { OriginNamespacedTag.REGEX.matchEntire(it.name) }
                        .forEach { (modifier, match) ->
                            if (match == null) return@forEach
                            instance.removeModifier(modifier)
                        }
                }
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

    private suspend fun unregisterAbilities(
        origin: Origin,
        player: Player
    ) {
        origin.activeKeybindAbilities[player].clear(Ability::unregister)
        origin.activePassiveAbilities[player].clear(Ability::unregister)
    }

    private suspend fun registerAbilities(
        origin: Origin,
        player: Player
    ) {
        origin.passiveAbilityGenerators
            .map { generator -> generator.of(player) }
            .onEach { passive -> origin.activePassiveAbilities.put(player, passive) }
            .forEach { passive -> passive.register() }

        origin.keybindAbilityGenerators.entries()
            .map { (keybind, generator) -> keybind to generator.of(player) }
            .onEach { (_, ability) -> origin.activeKeybindAbilities.put(player, ability) }
            .onEach { (keybind, ability) -> ability.activateWithKeybinding(keybind) }
            .forEach { (_, ability) -> ability.register() }
    }

    public enum class IgnoreReason(public val description: String) { GAMEMODE("Not in survival or adventure mode") }
}
