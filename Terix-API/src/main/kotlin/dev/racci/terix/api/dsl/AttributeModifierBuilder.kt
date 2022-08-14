package dev.racci.terix.api.dsl

import dev.racci.terix.api.OriginService
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.origin.OriginValues
import dev.racci.terix.api.origins.states.State
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import java.util.UUID
import kotlin.properties.Delegates

class AttributeModifierBuilder() {

    constructor(builder: AttributeModifierBuilder.() -> Unit) : this() {
        builder(this)
    }

    var uuid: UUID? = null
    var attribute by Delegates.notNull<Attribute>()
    var name by Delegates.notNull<String>()
    var amount by Delegates.notNull<Number>()
    var operation by Delegates.notNull<AttributeModifier.Operation>()

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

    fun build(): AttributeModifier = AttributeModifier(
        uuid ?: UUID.randomUUID(),
        name.takeUnless { it.isBlank() || !it.matches(regex) } ?: error("Invalid name. Was blank or didn't match ${regex.pattern}: $name"),
        amount as? Double ?: amount.toDouble(),
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
