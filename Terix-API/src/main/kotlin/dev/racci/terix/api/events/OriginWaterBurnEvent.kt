package dev.racci.terix.api.events

import dev.racci.minix.api.events.CompanionEventHandler
import dev.racci.terix.api.data.player.TerixPlayer
import dev.racci.terix.api.origins.origin.Origin
import org.bukkit.event.HandlerList

public class OriginWaterBurnEvent(
    player: TerixPlayer,
    origin: Origin,
    public var damage: Double
) : OriginEvent(player, origin) {
    public companion object : CompanionEventHandler() {
        @JvmStatic override fun getHandlerList(): HandlerList = super.getHandlerList()
    }
}
