package dev.racci.terix.api.dsl

import dev.racci.terix.api.OriginService
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.origin.OriginValues
import dev.racci.terix.api.origins.states.State
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import java.util.UUID

class AttributeModifierBuilder(
    uuid: UUID? = null,
    attribute: Attribute? = null,
    name: String? = null,
    amount: Number? = null,
    operation: AttributeModifier.Operation? = null
) : CachingBuilder<AttributeModifier>() {

    var uuid: UUID by createWatcher(uuid ?: UUID.randomUUID())
    var attribute: Attribute by createWatcher(attribute)
    var name: String by createWatcher(name)
    var amount: Double by createWatcher(amount?.toDouble())
    var operation: AttributeModifier.Operation by createWatcher(operation)

    inline fun <reified O : Origin> originName(state: State) = originName(OriginService.getOrigin(O::class), state)

    fun originName(
        origin: OriginValues,
        state: State
    ) = originName(origin.name, state.name)

    fun originName(
        origin: String,
        state: String
    ): AttributeModifierBuilder {
        name = Companion.originName(origin, state)
        return this
    }

    override fun create() = AttributeModifier(
        uuid,
        name.takeUnless { it.isBlank() || !it.matches(regex) } ?: error("Invalid name. Was blank or didn't match ${regex.pattern}: $name"),
        amount as? Double ?: amount,
        operation
    )

    companion object {
        val regex = Regex("origin_modifier_(?<origin>[a-z]+)_(?<state>[a-z]+)")

        fun originName(
            origin: Origin,
            state: State
        ) = originName(origin.name, state.name)

        fun originName(
            origin: String,
            trigger: String
        ) = "origin_modifier_${origin.lowercase()}_${trigger.lowercase()}"
    }
}
