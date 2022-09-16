package dev.racci.terix.core.services.runnables

import dev.racci.minix.api.utils.now
import dev.racci.minix.api.utils.ticks
import dev.racci.terix.api.origins.OriginHelper
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.states.State
import dev.racci.terix.core.extensions.inDarkness
import dev.racci.terix.core.extensions.wasInDarkness
import dev.racci.terix.core.services.RunnableService
import kotlinx.datetime.Instant
import org.bukkit.entity.Player

class DarknessTick(
    private val player: Player,
    private val origin: Origin,
    private val service: RunnableService,
    mother: MotherCoroutineRunnable
) : ChildCoroutineRunnable(mother) {

    private var lastTick = Instant.DISTANT_PAST
    private val damage = origin.damageTicks[State.LightState.DARKNESS]

    override suspend fun run() {
        service.doInvoke(player, origin, State.LightState.DARKNESS, player.wasInDarkness, player.inDarkness)
        if (OriginHelper.shouldIgnorePlayer(player)) return
        if (!player.inDarkness) return
        if ((now() - lastTick).ticks < 10) return

        if (damage == null) return

//        val damage = origin.damageTicks[State.LightState.DARKNESS] ?: return
//        if (false) return // TODO: Implement chance so it doesn't damage 4 times a second

        service.sync { player.damage(damage) }
    }
}
