package dev.racci.terix.core.services.runnables

import dev.racci.minix.api.extensions.isDay
import dev.racci.minix.api.utils.kotlin.ifTrue
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.core.extensions.inSunlight
import dev.racci.terix.core.extensions.wasInSunlight
import dev.racci.terix.core.services.HookService
import dev.racci.terix.core.services.RunnableService
import net.kyori.adventure.extra.kotlin.text
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.BlockPos
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack
import org.bukkit.entity.Player

class SunlightTick(
    private val player: Player,
    private val origin: AbstractOrigin,
    private val service: RunnableService,
    mother: MotherCoroutineRunnable,
) : ChildCoroutineRunnable(mother) {

    private var exposedTime: Int = 0

    override suspend fun run() {
        val burn = shouldTickSunlight(player)
        service.doInvoke(player, origin, Trigger.SUNLIGHT, player.wasInSunlight, player.inSunlight)

        when {
            !player.inSunlight && exposedTime > 0 -> exposedTime -= 5
            exposedTime < GRACE_PERIOD -> exposedTime += 5
        }
        showBar()
        if (!player.inSunlight || exposedTime < GRACE_PERIOD) return

        val ticks = origin.damageTicks[Trigger.SUNLIGHT] ?: return
        val helmet = player.inventory.helmet

        if (helmet == null) {
            if (player.fireTicks > ticks ||
                player.fireTicks > 0 &&
                !burn()
            ) return
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

    private fun showBar() {
        if (exposedTime < 20 || exposedTime > (GRACE_PERIOD - 1)) return player.sendActionBar(Component.empty())
        val index = (exposedTime / 20 - 1).coerceIn(cachedComponents.indices)
        player.sendActionBar(cachedComponents[index])
    }

    companion object {
        const val GRACE_CHAR = "▇"
        const val GRACE_PERIOD = 10 * 20
        private var cachedComponents = run {
            val multiplier = 20 / (GRACE_PERIOD / 20)
            Array<Component>(GRACE_PERIOD / 20) {
                text {
                    repeat(it * multiplier) {
                        append(Component.text(GRACE_CHAR, NamedTextColor.GOLD))
                    }
                }
            }.reversedArray()
        }

        fun shouldTickSunlight(player: Player): () -> Boolean {
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
            return shouldBurn
        }
    }
}
