package dev.racci.terix.api.events

import dev.racci.minix.api.events.CompanionEventHandler
import dev.racci.minix.api.events.player.KPlayerEvent
import dev.racci.terix.api.origins.origin.Origin
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList

/**
 * Called when a player tries to change origin.
 *
 * ## Note: This event will be called in a cancelled state sometimes.
 *
 * @param player The player that tried to change origin.
 * @property preOrigin The players current origin.
 * @property newOrigin The new origin.
 * @property bypassCooldown If the cooldown is bypassed.
 * @property skipRequirement If the origins' requirement check is skipped.
 */
public class PlayerOriginChangeEvent(
    player: Player,
    public val preOrigin: Origin,
    public val newOrigin: Origin,
    public val bypassCooldown: Boolean = false,
    public val skipRequirement: Boolean = false
) : KPlayerEvent(player, true) {
    public var result: Result = Result.SUCCESS

    public enum class Result { CURRENT_ORIGIN, ON_COOLDOWN, NO_PERMISSION, SUCCESS }

    public companion object : CompanionEventHandler() {
        @JvmStatic override fun getHandlerList(): HandlerList = super.getHandlerList()
    }
}
