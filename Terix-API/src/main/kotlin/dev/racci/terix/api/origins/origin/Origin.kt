package dev.racci.terix.api.origins.origin

import dev.racci.terix.api.events.PlayerOriginChangeEvent
import org.apiguardian.api.API
import org.bukkit.entity.Player

@API(status = API.Status.MAINTAINED, since = "1.0.0")
abstract class Origin : OriginBuilder() {

    /**
     * Checks if the player has permission for this origin.
     *
     * @param player The player to check.
     * @return True if the player has permission, false otherwise.
     */
    open suspend fun hasPermission(player: Player) = permission?.let(player::hasPermission) ?: true

    /** Called when Terix first registers this origin. */
    open suspend fun handleRegister() = Unit

    /**
     * Called when the player loads|joins|respawns as this origin.
     * Called after [handleBecomeOrigin] if the player is becoming this origin.
     */
    open suspend fun handleLoad(player: Player) = Unit

    /**
     * Called when the player first becomes this origin.
     * Called before [handleLoad].
     */
    open suspend fun handleBecomeOrigin(event: PlayerOriginChangeEvent) = Unit

    /** Called when the player changes from this origin. */
    open suspend fun handleChangeOrigin(event: PlayerOriginChangeEvent) = Unit

    /** Called each game tick. */
    open suspend fun onTick(player: Player) = Unit

    final override fun toString(): String {
        return "Origin(name='$name', item=$item, " +
            "abilities=$abilities, stateBlock=$stateBlocks, stateDamageTicks=$stateDamageTicks, " +
            "damageActions=$damageActions, customFoodProperties=$customFoodProperties, customFoodActions=$customFoodActions)"
    }
}
