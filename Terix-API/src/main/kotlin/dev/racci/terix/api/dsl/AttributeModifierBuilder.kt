package dev.racci.terix.api.dsl

import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.Trigger
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import java.util.UUID
import kotlin.properties.Delegates

class AttributeModifierBuilder() {

    constructor(builder: AttributeModifierBuilder.() -> Unit) : this() {
        builder()
    }

    private val uuid by lazy(UUID::randomUUID)

    var attribute by Delegates.notNull<Attribute>()
    var name by Delegates.notNull<String>()
    var amount by Delegates.notNull<Number>()
    var operation by Delegates.notNull<AttributeModifier.Operation>()

    fun originName(
        origin: AbstractOrigin,
        trigger: Trigger
    ) = originName(origin.name, trigger.name)

    fun originName(
        origin: String,
        trigger: String
    ): AttributeModifierBuilder {
        name = "origin_modifier_${origin.lowercase()}_${trigger.lowercase()}"
        return this
    }

    fun build(): AttributeModifier =
        AttributeModifier(
            uuid,
            name.takeUnless { it.isBlank() || !it.matches(regex) } ?: error("Invalid name. Was blank or didn't match ${regex.pattern}: $name"),
            amount.toDouble(),
            operation
        )

    companion object {
        val regex = Regex("origin_modifier_(?<origin>[a-z]+)_(?<trigger>[a-z]+)")
    }
}
