package dev.racci.terix.core.services.runnables

import dev.racci.minix.api.utils.now
import dev.racci.minix.api.utils.ticks
import dev.racci.terix.api.data.player.TerixPlayer
import dev.racci.terix.api.data.player.TickCache
import dev.racci.terix.api.origins.OriginHelper
import dev.racci.terix.api.origins.states.State
import kotlinx.datetime.Instant

public class RainTick(terixPlayer: TerixPlayer) : ChildTicker(terixPlayer, State.WeatherState.RAIN, TickCache::rain) {

    private var lastTick = Instant.DISTANT_PAST

    override suspend fun shouldRun(): Boolean {
        return player.ticks.rain.current() && !OriginHelper.shouldIgnorePlayer(player)
    }

    override suspend fun handleRun() {
        if ((now() - lastTick).ticks < 10) return
        player.origin.stateData[State.WeatherState.RAIN].damage.tap { damage ->
            lastTick = now()
            sync { player.damage(damage) }
        }
    }
}
