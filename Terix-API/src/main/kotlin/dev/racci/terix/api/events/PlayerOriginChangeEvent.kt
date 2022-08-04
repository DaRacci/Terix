package dev.racci.terix.api.events

import dev.racci.minix.api.events.KEvent
import dev.racci.minix.api.events.KPlayerEvent
import dev.racci.terix.api.origins.origin.AbstractOrigin
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList

/**
 * Called when a player tries to change origin.
 * ## Note: This event will be called in a cancelled state sometimes.
 *
 * @property preOrigin The players current origin.
 * @property newOrigin The new origin.
 * @property bypassCooldown If true, the cooldown will be bypassed.
 * @param player The player that tried to change origin.
 */
class PlayerOriginChangeEvent(
    player: Player,
    val preOrigin: AbstractOrigin,
    val newOrigin: AbstractOrigin,
    val bypassCooldown: Boolean = false
) : KPlayerEvent(player, true) {
    companion object {
        @JvmStatic
        fun getHandlerList(): HandlerList = KEvent.handlerMap[PlayerOriginChangeEvent::class]
    }
}
