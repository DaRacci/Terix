package dev.racci.terix.core.services.runnables

import dev.racci.minix.api.extensions.sync
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.core.extensions.inRain
import dev.racci.terix.core.extensions.wasInRain
import dev.racci.terix.core.services.RunnableService
import org.bukkit.entity.Player

class RainTick(
    private val player: Player,
    private val origin: AbstractOrigin,
    private val service: RunnableService,
    mother: MotherCoroutineRunnable
) : ChildCoroutineRunnable(mother) {

    override suspend fun run() {
        player.wasInRain = player.inRain
        player.inRain = player.isInRain
        service.doInvoke(player, origin, Trigger.RAIN, player.wasInRain, player.inRain)
        if (!player.inRain) return

        val wet = origin.damageTicks[Trigger.WET]
        val rain = origin.damageTicks[Trigger.RAIN]
        if (wet == null && rain == null) return

        // Get the one which isn't null or whichever is higher
        val ticks = if (wet == null) rain!! else if (rain == null) wet else maxOf(wet, rain)

        service.sync { player.damage(ticks) }
    }
}
