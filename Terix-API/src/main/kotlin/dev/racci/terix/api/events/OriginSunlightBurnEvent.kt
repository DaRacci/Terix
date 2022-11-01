package dev.racci.terix.api.events

import dev.racci.minix.api.events.CompanionEventHandler
import dev.racci.terix.api.origins.origin.Origin
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList

/**
 * Called when the Sunlight tick ignites a player.
 *
 * @param player The player that was ignited.
 * @param origin The origin that ignited the player.
 * @property ticks The amount of ticks the player will be on fire for.
 */
public class OriginSunlightBurnEvent(
    player: Player,
    origin: Origin,
    public var ticks: Int
) : OriginEvent(player, origin) {
    public companion object : CompanionEventHandler() {
        @JvmStatic override fun getHandlerList(): HandlerList = super.getHandlerList()
    }
}
