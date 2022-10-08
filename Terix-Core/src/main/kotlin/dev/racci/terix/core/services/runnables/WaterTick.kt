package dev.racci.terix.core.services.runnables

import dev.racci.minix.api.utils.now
import dev.racci.minix.api.utils.ticks
import dev.racci.terix.api.events.OriginWaterBurnEvent
import dev.racci.terix.api.origins.OriginHelper
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.states.State
import dev.racci.terix.core.extensions.inWater
import dev.racci.terix.core.services.RunnableService
import kotlinx.datetime.Instant
import org.bukkit.entity.Player

class WaterTick(
    player: Player,
    origin: Origin,
    private val service: RunnableService,
    mother: MotherCoroutineRunnable
) : ChildCoroutineRunnable(
    mother,
    player,
    origin,
    null,
    null,
    null
) {

    private var lastTick = Instant.DISTANT_PAST

    override suspend fun shouldRun(): Boolean {
        return player.inWater && (now() - lastTick).ticks >= 10 && OriginHelper.shouldIgnorePlayer(player)
    }

    override suspend fun handleRun() {
        val water = origin.stateDamageTicks[State.LiquidState.WATER] ?: return
        val event = OriginWaterBurnEvent(player, origin, water)

        if (!event.callEvent()) return

        lastTick = now()
        service.sync { player.damage(event.damage) }
    }
}
