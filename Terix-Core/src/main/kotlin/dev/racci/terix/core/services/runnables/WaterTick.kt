package dev.racci.terix.core.services.runnables

import dev.racci.minix.api.events.LiquidType
import dev.racci.minix.api.events.LiquidType.Companion.liquidType
import dev.racci.minix.api.extensions.sync
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.core.extensions.inWater
import dev.racci.terix.core.extensions.wasInWater
import dev.racci.terix.core.services.RunnableService
import org.bukkit.entity.Player

class WaterTick(
    private val player: Player,
    private val origin: AbstractOrigin,
    private val service: RunnableService,
    mother: MotherCoroutineRunnable
) : ChildCoroutineRunnable(mother) {

    override suspend fun run() {
        player.wasInWater = player.inWater
        player.inWater = player.location.block.liquidType == LiquidType.WATER
        service.doInvoke(player, origin, Trigger.WATER, player.wasInWater, player.inWater)
        if (!player.inWater) return

        val wet = origin.damageTicks[Trigger.WET]
        val water = origin.damageTicks[Trigger.WATER]
        if (wet == null && water == null) return

        // Get the one which isn't null or whichever is higher
        val ticks = if (wet == null) water!! else if (water == null) wet else maxOf(wet, water)

        service.sync { player.damage(ticks) }
    }
}
