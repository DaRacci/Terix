package dev.racci.terix.core.services.runnables

import arrow.core.getOrElse
import dev.racci.minix.api.utils.now
import dev.racci.minix.api.utils.ticks
import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.TerixPlayer.TickCache
import dev.racci.terix.api.events.OriginWaterBurnEvent
import dev.racci.terix.api.origins.OriginHelper
import dev.racci.terix.api.origins.states.State
import kotlinx.datetime.Instant

public class WaterTick(terixPlayer: TerixPlayer) : ChildTicker(terixPlayer, State.LiquidState.WATER, TickCache::water) {

    private var lastTick = Instant.DISTANT_PAST

    override suspend fun shouldRun(): Boolean {
        return player.ticks.water.current() && (now() - lastTick).ticks >= 10 && OriginHelper.shouldIgnorePlayer(player)
    }

    override suspend fun handleRun() {
        val water = player.origin.stateData[State.LiquidState.WATER].damage.getOrElse { return }
        val event = OriginWaterBurnEvent(player, player.origin, water)

        if (!event.callEvent()) return

        lastTick = now()
        sync { player.damage(event.damage) }
    }
}
