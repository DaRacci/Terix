package dev.racci.terix.core.services.runnables

import dev.racci.minix.api.utils.now
import dev.racci.minix.api.utils.ticks
import dev.racci.terix.api.origins.OriginHelper
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.states.State
import dev.racci.terix.core.extensions.inRain
import dev.racci.terix.core.extensions.wasInRain
import dev.racci.terix.core.services.RunnableService
import kotlinx.datetime.Instant
import org.bukkit.entity.Player

class RainTick(
    player: Player,
    origin: Origin,
    private val service: RunnableService,
    mother: MotherCoroutineRunnable
) : ChildCoroutineRunnable(
    mother,
    player,
    origin,
    State.WeatherState.RAIN,
    player::wasInRain,
    player::inRain
) {

    private var lastTick = Instant.DISTANT_PAST

    override suspend fun shouldRun(): Boolean {
        return player.inRain && !OriginHelper.shouldIgnorePlayer(player)
    }

    override suspend fun handleRun() {
        if ((now() - lastTick).ticks < 10) return
        val rain = origin.damageTicks[State.WeatherState.RAIN] ?: return

        lastTick = now()
        service.sync { player.damage(rain) }
    }
}
