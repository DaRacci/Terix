package dev.racci.terix.api.origins.abilities.passive

import dev.racci.terix.api.annotations.OriginEventSelector
import dev.racci.terix.api.events.abilities.LifeStealEvent
import dev.racci.terix.api.extensions.emptyLambdaThree
import dev.racci.terix.api.extensions.onSuccess
import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.origin.Origin
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent

public class LifeSteal(
    player: Player,
    origin: Origin,
) : PassiveAbility(player, origin) {
    public var maximumStolenHealth: Double = 2.0
    public var stolenPercentage: Float = 0.3F
    public var onLifeSteal: (Player, LivingEntity, Double) -> Unit = Unit.emptyLambdaThree()

    @OriginEventSelector(EventSelector.OFFENDER, EventPriority.MONITOR)
    public fun EntityDamageByEntityEvent.handle() {
        if (abilityPlayer.attackCooldown < 1.0F) return

        val target = entity as? LivingEntity ?: return
        val gainedHealth = getStolenPercentage(this.finalDamage)

        if (gainedHealth <= 0.0) return

        LifeStealEvent(this@LifeSteal, this, gainedHealth).onSuccess { event ->
            abilityPlayer.health += event.actualStolenAmount
            abilityPlayer.sendHealthUpdate()
            onLifeSteal(abilityPlayer, target, event.actualStolenAmount)
        }
    }

    private fun getStolenBonus(target: LivingEntity): Double {
        val amountTaken = (abilityPlayer.maxHealth - abilityPlayer.health).coerceAtMost(maximumStolenHealth)
        val vampAmount = target.health.coerceAtMost(amountTaken)
        return vampAmount
    }

    private fun getStolenPercentage(damage: Double) = (damage * stolenPercentage).coerceAtMost(maximumStolenHealth)
}
