package dev.racci.terix.api.dsl

import dev.racci.terix.api.data.OriginNamespacedTag
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

    override fun create(): AttributeModifier = AttributeModifier(
        uuid,
        name.takeUnless { it.isBlank() || !it.matches(OriginNamespacedTag.REGEX) } ?: error("Invalid name. Was blank or didn't match ${OriginNamespacedTag.REGEX.pattern}: $name"),
        amount as? Double ?: amount,
        operation
    )

    public operator fun invoke(player: Player) {
        player.getAttribute(attribute)?.addModifier(get())
    }
}
