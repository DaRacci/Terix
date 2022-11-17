package dev.racci.terix.api.origins.abilities.keybind

import dev.racci.minix.api.extensions.inWholeTicks
import dev.racci.terix.api.services.TickService
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import org.bukkit.event.Event
import org.bukkit.event.player.PlayerToggleSneakEvent
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration

public sealed class ChargeKeybindAbility : KeybindAbility() {
    private val perTick by lazy { 1F / (chargeTime.inWholeTicks / TickService.TICK_RATE) }
    private var holding: Boolean = false
    private val bossBar by lazy {
        BossBar.bossBar(
            Component.text(name).color(linkedOrigin.colour),
            0F,
            BossBar.Color.RED,
            BossBar.Overlay.PROGRESS
        )
    }

    /** The time it takes for the ability to reach full charge. */
    public abstract val chargeTime: Duration

    /** The current charge of the ability. */
    public abstract var charge: Float

    /**
     * Called when the ability fist starts charging.
     * This is only called when the ability starts charging from 0,
     * not when it is already partially charged, and the player starts charging again.
     */
    protected open suspend fun handleChargeStart(): Unit = Unit

    /**
     * Called when the player stops charging the ability.
     *
     * @param charge The amount of charge the ability had when the player stopped charging.
     */
    protected open suspend fun handleChargeRelease(charge: Float): Unit = Unit

    /**
     * Called each tick while the ability is charging.
     *
     * @param charge The percentage of the ability that is charged represented by a float between 0 and 1.
     */
    protected open suspend fun handleChargeIncrease(charge: Float): Unit = Unit

    /**
     * Called each tick while the ability is discharging.
     *
     * @param charge The percentage of the ability that is charged represented by a float between 0 and 1.
     */
    protected open suspend fun handleChargeDecrease(charge: Float): Unit = Unit

    final override suspend fun handleInternalGained() {
        TickService.filteredPlayer(abilityPlayer).map { _ ->
            val change = when {
                holding -> perTick
                charge > 0F -> -perTick
                else -> 0F
            }
            val newTotal = (charge + change).coerceIn(0F, 1F)
            if (newTotal != charge) {
                charge = newTotal
            }
            charge
        }.distinctUntilChanged().onEach { charge ->
            bossBar.progress(charge)
            if (charge > 0F) {
                abilityPlayer.showBossBar(bossBar)
            } else abilityPlayer.hideBossBar(bossBar)
        }.abilitySubscription()
    }

    final override suspend fun handleKeybind(event: Event) {
        when (event) {
            is PlayerToggleSneakEvent -> when {
                this.holding && !event.isSneaking -> {
                    this.holding = false
                    handleChargeRelease(charge)
                }
                !this.holding && event.isSneaking -> {
                    this.holding = true
                    handleChargeStart()
                }
            }
            else -> throw IllegalArgumentException("Event $event is not supported by this ability.")
        }
    }

    public inline fun observable(
        noinline onOvercharge: suspend () -> Unit = {},
        noinline onChange: suspend (property: KProperty<*>, oldValue: Float, newValue: Float) -> Unit
    ): ReadWriteProperty<Any?, Float> =
        object : ObservableProperty<Float>(0F) {
            override fun afterChange(
                property: KProperty<*>,
                oldValue: Float,
                newValue: Float
            ) = runBlocking { onChange(property, oldValue, newValue) }

            override fun beforeChange(
                property: KProperty<*>,
                oldValue: Float,
                newValue: Float
            ): Boolean = if (newValue > 1F) {
                runBlocking { onOvercharge() }
                false
            } else true
        }
}
