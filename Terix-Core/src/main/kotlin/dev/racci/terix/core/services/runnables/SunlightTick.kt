package dev.racci.terix.core.services.runnables

import dev.racci.minix.api.extensions.isDay
import dev.racci.minix.api.utils.kotlin.ifTrue
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.core.data.User.origin
import dev.racci.terix.core.extensions.inSunlight
import dev.racci.terix.core.extensions.wasInSunlight
import dev.racci.terix.core.services.HookService
import dev.racci.terix.core.services.RunnableService
import net.minecraft.core.BlockPos
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack
import org.bukkit.entity.Player

class SunlightTick(
    private val player: Player,
    private val origin: AbstractOrigin,
    private val service: RunnableService,
    mother: MotherCoroutineRunnable,
) : ChildCoroutineRunnable(mother) {

    override suspend fun run() {
        val bool = shouldTickSunlight(player)
        service.doInvoke(player, origin, Trigger.SUNLIGHT, player.wasInSunlight, player.inSunlight)

        if (!bool) return
        val ticks = origin.damageTicks[Trigger.SUNLIGHT] ?: return
        val helmet = player.inventory.helmet

        if (helmet == null) {
            if (player.fireTicks > ticks) return
            player.fireTicks = ticks.toInt()
            return
        }

        HookService.getService()
            .get<HookService.EcoEnchantsHook>()
            ?.sunResistance
            ?.let(helmet::hasEnchant)
            ?.ifTrue { return }

        val nms = player.toNMS()
        val amount = nms.random.nextInt(0, 2).takeIf { it != 0 } ?: return
        if (helmet.damage + amount > helmet.maxItemUseDuration) (helmet as CraftItemStack).handle.hurtAndBreak(amount, nms) {}
        (helmet as CraftItemStack).handle.hurt(amount, nms.level.random, nms)
    }

    companion object {

        fun shouldTickSunlight(player: Player): Boolean {
            val nms = player.toNMS()
            val brightness = nms.brightness
            val pos = BlockPos(nms.x, nms.eyeY, nms.z)
            val presentPrevention = player.isInWaterOrRainOrBubbleColumn || player.isInPowderedSnow
            val shouldBurn = { (nms.random.nextFloat() * 15.0f) < ((brightness - 0.4f) * 2.0f) } // Lazy evaluation
            val actuallyInSunlight = player.isDay &&
                !presentPrevention &&
                brightness > 0.5f &&
                nms.level.canSeeSky(pos)

            player.wasInSunlight = player.inSunlight
            player.inSunlight = actuallyInSunlight
            return actuallyInSunlight && shouldBurn()
        }
    }
}
