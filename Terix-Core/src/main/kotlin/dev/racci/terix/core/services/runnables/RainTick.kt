package dev.racci.terix.core.services.runnables

import dev.racci.minix.api.extensions.sync
import dev.racci.minix.api.utils.now
import dev.racci.minix.api.utils.ticks
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.core.extensions.inRain
import dev.racci.terix.core.extensions.wasInRain
import dev.racci.terix.core.services.RunnableService
import kotlinx.datetime.Instant
import org.bukkit.entity.Player

class RainTick(
    private val player: Player,
    private val origin: AbstractOrigin,
    private val service: RunnableService,
    mother: MotherCoroutineRunnable
) : ChildCoroutineRunnable(mother) {

    private var lastTick = Instant.DISTANT_PAST

    override suspend fun run() {
        service.doInvoke(player, origin, Trigger.RAIN, player.wasInRain, player.inRain)

        if (!player.inRain) return
        if ((now() - lastTick).ticks < 10) return

        val wet = origin.damageTicks[Trigger.WET]
        val rain = origin.damageTicks[Trigger.RAIN]
        if (wet == null && rain == null) return

        // Get the one which isn't null or whichever is higher
        val ticks = if (wet == null) rain!! else if (rain == null) wet else maxOf(wet, rain)
        lastTick = now()
        service.sync { player.damage(ticks) }
    }
}
