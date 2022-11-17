package dev.racci.terix.core.origins.abilities.passive

import dev.racci.minix.api.data.enums.LiquidType
import dev.racci.minix.api.events.player.PlayerLiquidEnterEvent
import dev.racci.minix.api.events.player.PlayerLiquidExitEvent
import dev.racci.minix.api.extensions.dropItem
import dev.racci.minix.api.extensions.taskAsync
import dev.racci.minix.api.scheduler.CoroutineTask
import dev.racci.minix.api.utils.now
import dev.racci.terix.api.annotations.OriginEventSelector
import dev.racci.terix.api.data.OriginNamespacedTag
import dev.racci.terix.api.data.OriginNamespacedTag.Companion.applyTag
import dev.racci.terix.api.dsl.AttributeModifierBuilder
import dev.racci.terix.api.dsl.dslMutator
import dev.racci.terix.api.origins.OriginHelper
import dev.racci.terix.api.origins.abilities.passive.PassiveAbility
import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.core.extensions.inWater
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack
import kotlin.reflect.KFunction0
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public class WaterHealthBonus(
    override val abilityPlayer: Player,
    override val linkedOrigin: Origin
) : PassiveAbility() {
    public val maxBonus: Int = 20
    public val damageCooldown: Duration = 8.seconds

    private var lastDamage: Instant = Instant.DISTANT_PAST
    private val modifierCache = Array(maxBonus + 1) { i ->
        dslMutator<AttributeModifierBuilder> {
            amount = convertIndex(i)
            operation = AttributeModifier.Operation.ADD_NUMBER
        }.asNew().applyTag(OriginNamespacedTag.abilityCustomOf(linkedOrigin, this@WaterHealthBonus)).get()
    }

    @OriginEventSelector(EventSelector.PLAYER)
    public fun PlayerLiquidEnterEvent.handle() {
        if (newType != LiquidType.WATER) return

        putEnterTask() {
            do {
                task(::inc) {
                    if (lastDamage == Instant.DISTANT_PAST || lastDamage + damageCooldown > now()) {
                        OriginHelper.increaseHealth(abilityPlayer, 0.5)
                    }
                }
            } while (currentModifier().amount < (maxBonus / 2.0) && player.inWater)
        }
    }

    @OriginEventSelector(EventSelector.PLAYER)
    public fun PlayerLiquidExitEvent.onExitLiquid() {
        if (previousType != LiquidType.WATER) return

        putExitTask {
            while (currentModifier().amount > 0.0 && !abilityPlayer.inWater) {
                task(::dec)
            }
        }
    }

    private suspend fun task(
        function: KFunction0<Unit>,
        extraAction: () -> Unit = {}
    ) {
        val instance = abilityPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!

        instance.removeModifier(currentModifier())
        function.call()
        instance.addModifier(currentModifier())

        extraAction()

        delay(250)
    }

    @OriginEventSelector(EventSelector.ENTITY, EventPriority.MONITOR)
    public fun EntityDamageEvent.slimeballDrop() {
        val now = now()
        if (lastDamage != Instant.DISTANT_PAST && (lastDamage + 15.seconds) > now) return

        lastDamage = now
        this.entity.location.dropItem(ItemStack(Material.SLIME_BALL))
    }

    private var currentBonus: Int = 0
    private var runningTask: Pair<CoroutineTask, Boolean>? = null

    private fun currentModifier(): AttributeModifier = this.modifierCache[this.currentBonus]

    public fun inc() {
        currentBonus = convertAmount(this.currentModifier().amount) + 1
    }

    public fun dec() {
        currentBonus = convertAmount(this.currentModifier().amount) - 1
    }

    public fun putEnterTask(block: suspend () -> Unit) {
        if (runningTask?.second == true) return
        runningTask = newTask(block) to true
    }

    public fun putExitTask(block: suspend () -> Unit) {
        if (runningTask?.second == false) return
        runningTask?.first?.cancel()
        runningTask = newTask(block) to false
    }

    private fun newTask(block: suspend () -> Unit) = taskAsync(1.seconds) {
        block()
        runningTask = null
    }

    internal fun convertIndex(index: Int): Double = (index / 2.0)

    internal fun convertAmount(amount: Double): Int = (amount * 2).toInt()
}
