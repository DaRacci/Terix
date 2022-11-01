package dev.racci.terix.core.services.runnables

import dev.racci.minix.api.utils.now
import dev.racci.minix.api.utils.ticks
import dev.racci.terix.api.origins.OriginHelper
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.states.State
import dev.racci.terix.core.extensions.inDarkness
import dev.racci.terix.core.extensions.wasInDarkness
import kotlinx.datetime.Instant
import org.bukkit.entity.Player

public class DarknessTick(
    player: Player,
    origin: Origin
) : ChildTicker(
    player,
    origin,
    State.LightState.DARKNESS,
    player::wasInDarkness,
    player::inDarkness
) {
    private var lastTick = Instant.DISTANT_PAST
    private val damage = origin.stateDamageTicks[State.LightState.DARKNESS]

    override suspend fun handleRun() {
        if (OriginHelper.shouldIgnorePlayer(player)) return
        if (!player.inDarkness) return
        if ((now() - lastTick).ticks < 10) return

        if (damage == null) return

//        val damage = origin.stateDamageTicks[State.LightState.DARKNESS] ?: return
//        if (false) return // TODO: Implement chance so it doesn't damage 4 times a second

        sync { player.damage(damage) }
    }
}
