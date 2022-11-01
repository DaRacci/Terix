package dev.racci.terix.api.origins.origin

import dev.racci.terix.api.events.PlayerOriginChangeEvent
import org.apiguardian.api.API
import org.bukkit.entity.Player

// TODO -> Player levels
// TODO -> Per player instances of origins
// TODO -> Potion modifier system (eg preventing potion effects and modifying them)
// TODO -> Attribute modifier system (eg preventing attribute modifiers and modifying them)
@API(status = API.Status.MAINTAINED, since = "1.0.0")
public abstract class Origin : OriginBuilder() {

    /**
     * Checks if the player has permission for this origin.
     *
     * @param player The player to check.
     * @return True if the player has permission, false otherwise.
     */
    public open suspend fun hasPermission(player: Player): Boolean = permission?.let(player::hasPermission) ?: true

    /** Called when Terix first registers this origin. */
    public open suspend fun handleRegister(): Unit = Unit

    /**
     * Called when the player loads|joins|respawns as this origin.
     * Called after [handleBecomeOrigin] if the player is becoming this origin.
     */
    public open suspend fun handleLoad(player: Player): Unit = Unit

    /** Called when the server is stopping or plugin is being unloaded and caches should be cleaned. */
    public open suspend fun handleUnload(): Unit = Unit

    /**
     * Called when the player first becomes this origin.
     * Called before [handleLoad].
     */
    public open suspend fun handleBecomeOrigin(event: PlayerOriginChangeEvent): Unit = Unit

    /** Called when the player changes from this origin. */
    public open suspend fun handleChangeOrigin(event: PlayerOriginChangeEvent): Unit = Unit

    /** When the player changes to gm 1 for example. */
    public open suspend fun handleDeactivate(player: Player): Unit = Unit

    /** Called each game tick. */
    public open suspend fun onTick(player: Player): Unit = Unit

    final override fun toString(): String {
        return "Origin(name='$name', item=$item, " +
            "abilities=$abilities, stateBlock=$stateBlocks, stateDamageTicks=$stateDamageTicks, " +
            "damageActions=$damageActions, customFoodProperties=$customFoodProperties, customFoodActions=$customFoodActions)"
    }
}
