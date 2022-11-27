package dev.racci.terix.api.origins.origin.builder

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import dev.racci.terix.api.origins.origin.OriginValues
import dev.racci.terix.api.origins.states.State
import org.bukkit.event.entity.EntityDamageEvent

public class DamageBuilderPart internal constructor() : BuilderPart<Either<DamageBuilderPart.DamageActionElement, DamageBuilderPart.DamageTickElement>>() {

    /**
     * Deals the amount of damage to the player when the given trigger is
     * activated.
     *
     * @param value The amount of damage to deal.
     * @receiver The trigger to activate the damage.
     */
    public operator fun State.plusAssign(value: Double) {
        DamageTickElement(
            this,
            value
        ).right().let(::addElement)
    }

    /**
     * Calls this lambda when a damage event with the cause is called.
     *
     * @param lambda
     */
    public operator fun EntityDamageEvent.DamageCause.plusAssign(lambda: suspend (EntityDamageEvent) -> Unit) {
        DamageActionElement(
            this,
            lambda
        ).left().let(::addElement)
    }

    /**
     * Adds this amount of damage to the player when the player's damage cause
     * is this.
     *
     * @param value The amount of damage to add.
     * @receiver The damage cause that is affected.
     */
    public operator fun EntityDamageEvent.DamageCause.plusAssign(value: Double) {
        DamageActionElement(
            this
        ) { this.damage += value }.left().let(::addElement)
    }

    /**
     * Minuses this amount of damage to the player when the player's damage
     * cause is this.
     *
     * @param value The amount of damage to minus.
     * @receiver The damage cause that is affected.
     */
    public operator fun EntityDamageEvent.DamageCause.minusAssign(value: Double) {
        DamageActionElement(
            this
        ) { this.damage -= value }.left().let(::addElement)
    }

    /**
     * Multiplies this amount of damage to the player when the player's damage
     * cause is this.
     *
     * @param value The amount of damage to multiply.
     * @receiver The damage cause that is affected.
     */
    public operator fun EntityDamageEvent.DamageCause.timesAssign(value: Double) {
        DamageActionElement(
            this
        ) { this.damage *= value }.left().let(::addElement)
    }

    /**
     * Divides this amount of damage to the player when the player's damage
     * cause is this.
     *
     * @param value The amount of damage to divide.
     * @receiver The damage cause that is affected.
     */
    public operator fun EntityDamageEvent.DamageCause.divAssign(value: Double) {
        DamageActionElement(
            this
        ) { this.damage /= value }.left().let(::addElement)
    }

    /**
     * Adds all elements to [State.plusAssign]
     *
     * @param value The amount of damage to deal.
     * @receiver The triggers that activate the damage.
     */
    public operator fun Collection<State>.plusAssign(value: Double): Unit = forEach { it += value }

    /**
     * Adds all elements to [EntityDamageEvent.DamageCause.plusAssign]
     *
     * @param value The amount of damage to deal.
     * @receiver The causes that are affected.
     */
    @JvmName("plusAssignEntityDamageEventDamageCause")
    public operator fun Collection<EntityDamageEvent.DamageCause>.plusAssign(value: Double): Unit = forEach { it += value }

    /** Adds all elements to [EntityDamageEvent.DamageCause.minusAssign]. */
    public operator fun Collection<EntityDamageEvent.DamageCause>.minusAssign(value: Double): Unit = forEach { it -= value }

    /**
     * Adds all elements to [EntityDamageEvent.DamageCause.timesAssign]
     *
     * @param value The amount of damage to multiply.
     * @receiver The triggers that activate the damage.
     */
    public operator fun Collection<EntityDamageEvent.DamageCause>.timesAssign(value: Double): Unit = forEach { it *= value }

    /**
     * Adds all elements to [EntityDamageEvent.DamageCause.divAssign]
     *
     * @param value The amount of damage to divide.
     * @receiver The triggers that activate the damage.
     */
    public operator fun Collection<EntityDamageEvent.DamageCause>.divAssign(value: Double): Unit = forEach { it /= value }

    override suspend fun insertInto(originValues: OriginValues): Option<Exception> {
        val damageActions = originValues.damageActions.builder()

        super.getElements().forEach { either ->
            either.fold(
                ifLeft = { (cause, action) ->
                    val existingLambda = damageActions[cause]
                    if (existingLambda != null) {
                        damageActions[cause] = {
                            existingLambda()
                            action()
                        }
                    } else damageActions[cause] = action
                },
                ifRight = { (state, amount) ->
                    originValues.stateData = originValues.stateData.modify(state, OriginValues.StateData::damage) { damage ->
                        damage.fold(
                            ifEmpty = { amount },
                            ifSome = { it + amount }
                        ).toOption()
                    }
                }
            )
        }

        originValues.damageActions = damageActions.build()

        return None
    }

    public data class DamageActionElement internal constructor(
        val cause: EntityDamageEvent.DamageCause,
        val block: suspend EntityDamageEvent.() -> Unit
    )

    public data class DamageTickElement internal constructor(
        val state: State,
        val damage: Double
    )
}
