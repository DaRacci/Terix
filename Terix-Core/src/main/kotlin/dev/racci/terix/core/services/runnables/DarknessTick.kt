package dev.racci.terix.core.services.runnables

import dev.racci.minix.api.extensions.sync
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.api.origins.origin.AbstractOrigin
import dev.racci.terix.core.extensions.inDarkness
import dev.racci.terix.core.extensions.wasInDarkness
import dev.racci.terix.core.services.RunnableService
import org.bukkit.entity.Player

class DarknessTick(
    private val player: Player,
    private val origin: AbstractOrigin,
    private val service: RunnableService,
    mother: MotherCoroutineRunnable
) : ChildCoroutineRunnable(mother) {

    override suspend fun run() {
        service.doInvoke(player, origin, Trigger.DARKNESS, player.wasInDarkness, player.inDarkness)
        if (!player.inDarkness) return

        val damage = origin.damageTicks[Trigger.DARKNESS] ?: return
        if (false) return // TODO: Implement chance so it doesn't damage 4 times a second

        service.sync { player.damage(damage) }
    }
}
