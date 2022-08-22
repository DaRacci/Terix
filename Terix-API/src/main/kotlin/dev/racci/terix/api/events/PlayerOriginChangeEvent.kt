package dev.racci.terix.api.events

import dev.racci.minix.api.events.KEvent
import dev.racci.minix.api.events.KPlayerEvent
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
class PlayerOriginChangeEvent(
    player: Player,
    val preOrigin: Origin,
    val newOrigin: Origin,
    val bypassCooldown: Boolean = false,
    val skipRequirement: Boolean = false
) : KPlayerEvent(player, true) {
    var result = Result.SUCCESS

    enum class Result { CURRENT_ORIGIN, ON_COOLDOWN, NO_PERMISSION, SUCCESS }

    companion object {
        @JvmStatic
        fun getHandlerList(): HandlerList = KEvent.handlerMap[PlayerOriginChangeEvent::class]
    }
}
