package dev.racci.terix.core.services.runnables

import dev.racci.minix.api.utils.now
import dev.racci.minix.api.utils.ticks
import dev.racci.terix.api.data.player.TerixPlayer
import dev.racci.terix.api.data.player.TickCache
import dev.racci.terix.api.origins.OriginHelper
import dev.racci.terix.api.origins.states.State
import kotlinx.datetime.Instant

public class DarknessTick(terixPlayer: TerixPlayer) : ChildTicker(terixPlayer, State.LightState.DARKNESS, TickCache::darkness) {
    private var lastTick = Instant.DISTANT_PAST
    private val damage = player.origin.stateData[State.LightState.DARKNESS].damage

    override suspend fun handleRun() {
        if (OriginHelper.shouldIgnorePlayer(player)) return
        if (!player.ticks.darkness.current()) return
        if ((now() - lastTick).ticks < 10) return

        damage.tap { damage -> sync { player.damage(damage) } }
//        val damage = origin.stateDamageTicks[State.LightState.DARKNESS] ?: return
//        if (false) return // TODO: Implement chance so it doesn't damage 4 times a second
    }
}
