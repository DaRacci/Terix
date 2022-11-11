package dev.racci.terix.api.origins.abilities.passives

import dev.racci.terix.api.annotations.OriginEventSelector
import dev.racci.terix.api.extensions.emptyLambdaThree
import dev.racci.terix.api.origins.abilities.PassiveAbility
import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.origin.Origin
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent

public class LifeSteal(
    player: Player,
    origin: Origin,
) : PassiveAbility(player, origin) {
    public var maximumStolenHealth: Double = 2.0
    public var onLifeSteal: (Player, LivingEntity, Double) -> Unit = Unit.emptyLambdaThree()

    @OriginEventSelector(EventSelector.OFFENDER)
    public fun EntityDamageByEntityEvent.handle() {
        logger.debug { "Handling life steal event" }
        if (abilityPlayer.attackCooldown < 1.0F) return
        val target = entity as? LivingEntity ?: return

        val (stolenAmount, _) = getStolenAmount(target)

        logger.debug { "Stolen amount: $stolenAmount" }

        abilityPlayer.health += stolenAmount
        abilityPlayer.sendHealthUpdate()
        onLifeSteal(abilityPlayer, target, stolenAmount)

        this.damage += stolenAmount
        (target as? Player)?.sendHealthUpdate()
    }

    /** Returns the amount stolen paired to if it will kill the target. */
    private fun getStolenAmount(target: LivingEntity): Pair<Double, Boolean> {
        val amountTaken = (abilityPlayer.maxHealth - abilityPlayer.health).coerceAtMost(maximumStolenHealth)
        println("Amount taken: $amountTaken")
        val vampAmount = target.health.coerceAtMost(amountTaken)
        println("Vamp amount: $vampAmount")
        return vampAmount to (target.health - vampAmount <= 0.0)
    }
}
