package dev.racci.terix.api.dsl

import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.api.origins.origin.AbstractOrigin
import dev.racci.terix.api.origins.origin.OriginValues
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

    fun originName(
        origin: OriginValues,
        trigger: Trigger
    ) = originName(origin.name, trigger.name)

    fun originName(
        origin: String,
        trigger: String
    ): AttributeModifierBuilder {
        name = Companion.originName(origin, trigger)
        return this
    }

    fun build(): AttributeModifier =
        AttributeModifier(
            uuid ?: UUID.randomUUID(),
            name.takeUnless { it.isBlank() || !it.matches(regex) } ?: error("Invalid name. Was blank or didn't match ${regex.pattern}: $name"),
            amount as? Double ?: amount.toDouble(),
            operation
        )

    companion object {
        val regex = Regex("origin_modifier_(?<origin>[a-z]+)_(?<trigger>[a-z]+)")

        fun originName(
            origin: AbstractOrigin,
            trigger: Trigger
        ) = originName(origin.name, trigger.name)

        fun originName(
            origin: String,
            trigger: String
        ) = "origin_modifier_${origin.lowercase()}_${trigger.lowercase()}"
    }
}
