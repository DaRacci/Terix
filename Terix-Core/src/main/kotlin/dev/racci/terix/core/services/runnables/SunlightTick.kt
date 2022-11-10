package dev.racci.terix.core.services.runnables

import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.events.OriginSunlightBurnEvent
import dev.racci.terix.api.origins.OriginHelper
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.states.State
import dev.racci.terix.api.services.TickService
import dev.racci.terix.core.extensions.inSunlight
import dev.racci.terix.core.extensions.wasInSunlight
import net.kyori.adventure.extra.kotlin.text
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack
import org.bukkit.entity.Player

public class SunlightTick(
    player: Player,
    origin: Origin
) : ChildTicker(
    player,
    origin,
    State.LightState.SUNLIGHT,
    player::wasInSunlight,
    player::inSunlight
) {

    private var exposedTime: Int = 0

    override suspend fun shouldRun(): Boolean {
        if (OriginHelper.shouldIgnorePlayer(player)) return false

        this.calculateBar()
        return this.player.inSunlight && exposedTime == GRACE_PERIOD
    }

    override suspend fun handleRun() {
        val ticks = origin.stateDamageTicks[State.LightState.SUNLIGHT]?.toInt() ?: return
        val helmet = player.inventory.helmet

        val event = OriginSunlightBurnEvent(player, origin, ticks)
        if (!event.callEvent()) return

        if (helmet != null) {
            val nms = player.toNMS()
            val amount = nms.random.nextInt(0, 2).takeIf { it != 0 } ?: return
            if (helmet.damage + amount > helmet.maxItemUseDuration) (helmet as CraftItemStack).handle.hurtAndBreak(amount, nms) {}
            (helmet as CraftItemStack).handle.hurt(amount, nms.level.random, nms)

            return
        }

        if (player.fireTicks > ticks || player.fireTicks > 0 && !shouldTickSunlight(player)) return
        player.fireTicks = ticks
    }

    private fun calculateBar() {
        exposedTime += when {
            !player.inSunlight && exposedTime > 0 -> -TickService.TICK_RATE
            player.inSunlight && exposedTime < GRACE_PERIOD -> TickService.TICK_RATE
            else -> 0
        }

        this.showBar()
    }

    private fun showBar() {
        if (exposedTime < TickService.TICK_RATE || exposedTime == GRACE_PERIOD) return player.sendActionBar(Component.empty())
        val index = (exposedTime / 20 - 1).coerceIn(cachedComponents.indices)
        player.sendActionBar(cachedComponents[index])
    }

    private companion object {
        const val GRACE_CHAR = "▇"
        const val GRACE_PERIOD = 10 * 20
        var cachedComponents = run {
            val multiplier = 20 / (GRACE_PERIOD / 20)
            Array<Component>(GRACE_PERIOD / 20) {
                text {
                    repeat(it * multiplier) {
                        append(Component.text(GRACE_CHAR, NamedTextColor.GOLD))
                    }
                }
            }.reversedArray()
        }

        fun shouldTickSunlight(player: Player): Boolean {
            val brightness = player.location.toCenterLocation().block.lightLevel
            return (player.toNMS().random.nextFloat() * 15.0f) < ((brightness - 0.4f) * 2.0f)
        }
    }
}
