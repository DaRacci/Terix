package dev.racci.terix.api.origins.origin.builder

import arrow.core.None
import arrow.core.Option
import dev.racci.terix.api.data.OriginNamespacedTag
import dev.racci.terix.api.data.OriginNamespacedTag.Companion.applyTag
import dev.racci.terix.api.dsl.AttributeModifierBuilder
import dev.racci.terix.api.origins.origin.OriginValues
import dev.racci.terix.api.origins.states.State
import kotlinx.collections.immutable.toPersistentSet
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier

public class AttributeBuilderPart internal constructor() : BuilderPart<AttributeBuilderPart.AttributeElement>() {

    /**
     * Removes this number from the players' base attributes.
     *
     * @param value The amount to remove.
     * @receiver The attribute to remove from.
     */
    public operator fun Attribute.minusAssign(value: Double): Unit = Pair(State.CONSTANT, this).minusAssign(value)

    /**
     * Adds this number to the players' base attributes.
     *
     * @param value The amount to add.
     * @receiver The attribute to add to.
     */
    public operator fun Attribute.plusAssign(value: Double): Unit = Pair(State.CONSTANT, this).plusAssign(value)

    /**
     * Multiplies the players' base attribute by this number.
     *
     * @param value The amount to multiply by.
     * @receiver The attribute to multiply.
     */
    public operator fun Attribute.timesAssign(value: Double): Unit = Pair(State.CONSTANT, this).timesAssign(value)

    /**
     * Divides the players base attribute by this number.
     *
     * @param value The amount to divide by.
     * @receiver The attribute to divide.
     */
    public operator fun Attribute.divAssign(value: Double): Unit = Pair(State.CONSTANT, this).divAssign(value)

    /**
     * Removes this number from the players' attribute when this trigger is active.
     *
     * @param value The amount to remove.
     * @receiver The Trigger and Attribute to remove from.
     */
    public operator fun Pair<State, Attribute>.minusAssign(value: Number): Unit = AttributeElement.of(this, value, AttributeModifier.Operation.ADD_NUMBER).let(::addElement)

    /**
     * Adds this number to the players' attribute when this trigger is active.
     *
     * @param value The amount to add.
     * @receiver The Trigger and Attribute to add to.
     */
    public operator fun Pair<State, Attribute>.plusAssign(value: Number): Unit = AttributeElement.of(this, value, AttributeModifier.Operation.ADD_NUMBER).let(::addElement)

    /**
     * Multiplies the players attribute by this number when this trigger is active.
     *
     * @param value The amount to multiply by.
     * @receiver The Trigger and Attribute to multiply.
     */
    public operator fun Pair<State, Attribute>.timesAssign(value: Double): Unit = AttributeElement.of(this, value - 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1).let(::addElement)

    /**
     * Divides the players attribute by this number when this trigger is active.
     *
     * @param value The amount to divide by.
     * @receiver The Trigger and Attribute to divide.
     */
    public operator fun Pair<State, Attribute>.divAssign(value: Double): Unit = AttributeElement.of(this, 1.0 / value, AttributeModifier.Operation.MULTIPLY_SCALAR_1).let(::addElement)

    override suspend fun insertInto(originValues: OriginValues): Option<Exception> {
        super.getElements().groupBy(AttributeElement::state).map { (state, elements) ->
            state to elements.map { element -> element.toBuilder(originValues) }.toPersistentSet()
        }.filter { (_, modifiers) ->
            modifiers.isNotEmpty()
        }.forEach { (state, modifiers) ->
            originValues.stateData = originValues.stateData.modify(state, OriginValues.StateData::modifiers) { modifiers }
        }

        return None
    }

    public data class AttributeElement internal constructor(
        val state: State,
        val amount: Double,
        val attribute: Attribute,
        val operation: AttributeModifier.Operation
    ) {
        public fun toBuilder(
            originValues: OriginValues
        ): AttributeModifierBuilder = AttributeModifierBuilder(
            null,
            attribute,
            null,
            amount,
            operation
        ).applyTag(OriginNamespacedTag.baseStateOf(originValues, state))

        internal companion object {
            fun of(
                pair: Pair<State, Attribute>,
                amount: Number,
                operation: AttributeModifier.Operation
            ) = AttributeElement(pair.first, amount.toDouble(), pair.second, operation)
        }
    }
}
