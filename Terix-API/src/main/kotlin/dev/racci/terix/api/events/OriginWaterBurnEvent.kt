package dev.racci.terix.api.events

import dev.racci.minix.api.events.CompanionEventHandler
import dev.racci.terix.api.origins.origin.Origin
import org.bukkit.entity.Player

class OriginWaterBurnEvent(
    player: Player,
    origin: Origin,
    var damage: Double
) : OriginEvent(player, origin) {
    companion object : CompanionEventHandler() {
        @JvmStatic override fun getHandlerList() = super.getHandlerList()
    }
}
