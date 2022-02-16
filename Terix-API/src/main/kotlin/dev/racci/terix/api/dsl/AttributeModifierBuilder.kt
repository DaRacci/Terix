package dev.racci.terix.api.dsl

import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import java.util.UUID
import kotlin.properties.Delegates

class AttributeModifierBuilder {

    private val uuid by lazy(UUID::randomUUID)

    var attribute by Delegates.notNull<Attribute>()
    var name by Delegates.notNull<String>()
    var amount by Delegates.notNull<Double>()
    var operation by Delegates.notNull<AttributeModifier.Operation>()

    fun build(): AttributeModifier =
        AttributeModifier(
            uuid,
            name,
            amount,
            operation
        )
}
