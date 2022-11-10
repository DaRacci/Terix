package dev.racci.terix.api.origins

import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.extensions.collections.clear
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.Terix
import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.dsl.AttributeModifierBuilder
import dev.racci.terix.api.dsl.PotionEffectBuilder
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
            activeStates.forEach { newOrigin.statePotions[it]?.let(player::addPotionEffects) }
//            when {
//                oldOrigin?.nightVision == true && !newOrigin.nightVision -> {
//                    removePotions += PotionEffectType.NIGHT_VISION
//                }
//                oldOrigin?.nightVision != true && newOrigin.nightVision -> {
//                    val state = activeStates.find { transaction { TerixPlayer[player].nightVision == it } } ?: return@sync
//                    player.addPotionEffect(nightVisionPotion(newOrigin, state))
//                }
//            }
        }

        activeStates.forEach { it.activate(player, newOrigin) }
        if (player.health < curHealth) player.health = curHealth.coerceAtMost(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value)
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
            origin.passiveAbilities
                .map { generator -> generator.of(player) }
                .onEach { passive -> origin.activePassiveAbilities.put(player, passive) }
                .forEach { passive -> passive.register() }
        }
    }

    /** Designed to be invoked for when a player needs everything disabled. */
    public suspend fun deactivateOrigin(player: Player) {
        getOriginPotions(player, null)
            .onEach { logger.trace { "Removing potion effect $it from ${player.name}" } }
            .forEach(player::removePotionEffect)

        async {
            val origin = TerixPlayer.cachedOrigin(player)
            origin.handleDeactivate(player)
            origin.abilities.values.forEach { it.deactivate(player) }
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

            origin.activePassiveAbilities[player].clear { unregister() }
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
    public fun getOriginPotions(
        player: Player,
        state: State?
    ): Sequence<PotionEffectType> = player.activePotionEffects
        .asSequence()
        .mapNotNull { effect ->
            val match = PotionEffectBuilder.regex.find(effect.key?.asString().orEmpty()) ?: return@mapNotNull null
            if (!(match.groups["type"]?.value == "potion" && state != null && match.groups["state"]?.value == state.name.lowercase())) return@mapNotNull null
            effect.type
        }

    public fun potionState(potion: PotionEffect): State? {
        val match = PotionEffectBuilder.regex.find(potion.key?.asString().orEmpty()) ?: return null
        return State.values.find { it.name.lowercase() == match.groups["state"]?.value }
    }

    public fun potionOrigin(potion: PotionEffect): Origin? {
        val match = PotionEffectBuilder.regex.find(potion.key?.asString().orEmpty()) ?: return null
        return OriginService.getOriginOrNull(match.groups["from"]?.value)
    }

    public enum class IgnoreReason(public val description: String) { GAMEMODE("Not in survival or adventure mode") }
}
