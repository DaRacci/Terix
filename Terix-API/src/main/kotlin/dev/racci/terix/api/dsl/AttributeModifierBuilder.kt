package dev.racci.terix.api.dsl

import dev.racci.terix.api.OriginService
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.origin.OriginValues
import dev.racci.terix.api.origins.states.State
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import java.util.UUID

public class AttributeModifierBuilder(
    uuid: UUID? = null,
    attribute: Attribute? = null,
    name: String? = null,
    amount: Number? = null,
    operation: AttributeModifier.Operation? = null
) : CachingBuilder<AttributeModifier>() {

    public var uuid: UUID by createWatcher(uuid ?: UUID.randomUUID())
    public var attribute: Attribute by createWatcher(attribute)
    public var name: String by createWatcher(name)
    public var amount: Double by createWatcher(amount?.toDouble())
    public var operation: AttributeModifier.Operation by createWatcher(operation)

    public inline fun <reified O : Origin> originName(state: State): AttributeModifierBuilder = originName(OriginService.getOrigin(O::class), state)

    public fun originName(
        origin: OriginValues,
        state: State
    ): AttributeModifierBuilder = originName(origin.name, state.name)

    public fun originName(
        origin: String,
        state: String
    ): AttributeModifierBuilder {
        name = Companion.originName(origin, state)
        return this
    }

    override fun create(): AttributeModifier = AttributeModifier(
        uuid,
        name.takeUnless { it.isBlank() || !it.matches(regex) } ?: error("Invalid name. Was blank or didn't match ${regex.pattern}: $name"),
        amount as? Double ?: amount,
        operation
    )

    public companion object {
        public val regex: Regex = Regex("origin_modifier_(?<origin>[a-z]+)_(?<state>[a-z]+)")

        public fun originName(
            origin: Origin,
            state: State
        ): String = originName(origin.name, state.name)

        public fun originName(
            origin: String,
            trigger: String
        ): String = "origin_modifier_${origin.lowercase()}_${trigger.lowercase()}"
    }
}
