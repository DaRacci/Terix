@file:Suppress("unused")
@file:JvmName("OriginAttribute")
package me.racci.sylphia.origins

import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier

class OriginAttribute(var attribute: Attribute, var value: Double)

data class BaseAttribute(var attribute: Attribute, var double: Double)

data class AttributeModifier(var attribute: Attribute, var modifier: AttributeModifier)