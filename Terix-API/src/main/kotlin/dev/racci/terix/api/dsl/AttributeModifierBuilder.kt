package dev.racci.terix.api.dsl

import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.terix.api.data.OriginNamespacedTag
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.attribute.AttributeModifier
import org.bukkit.craftbukkit.v1_19_R1.attribute.CraftAttributeInstance
import org.bukkit.entity.Player
import java.util.UUID
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible
import net.minecraft.world.entity.ai.attributes.AttributeInstance as NMSAttributeInstance

// TODO -> Maybe just convert to immutable data classes?
public class AttributeModifierBuilder(
    uuid: UUID? = null,
    attribute: Attribute? = null,
    name: String? = null,
    amount: Number? = null,
    operation: AttributeModifier.Operation? = null
) : CachingBuilder<AttributeModifier>() {

    public var uuid: UUID by createWatcher(uuid ?: UUID.randomUUID())
    public var attribute: Attribute by createLockingWatcher(attribute)
    public var name: String by createLockingWatcher(name)
    public var amount: Double by createWatcher(amount?.toDouble())
    public var operation: AttributeModifier.Operation by createWatcher(operation)

    override fun create(): AttributeModifier = AttributeModifier(
        uuid,
        name.takeUnless { it.isBlank() || !it.matches(OriginNamespacedTag.REGEX) } ?: error("Invalid name. Was blank or didn't match ${OriginNamespacedTag.REGEX.pattern}: $name"),
        amount as? Double ?: amount,
        operation
    )

    // TODO: PR
    private val craftHandle: KProperty1<in AttributeInstance, NMSAttributeInstance> = CraftAttributeInstance::class.declaredMemberProperties
        .first { it.name == "handle" }
        .apply { isAccessible = true }
        .castOrThrow()

    public operator fun invoke(player: Player) {
        val attributeInstance = player.getAttribute(attribute) ?: error("Player doesn't have attribute $attribute")
        craftHandle.get(attributeInstance).addTransientModifier(CraftAttributeInstance.convert(get()))
    }

    public fun remove(player: Player) {
        val attributeInstance = player.getAttribute(attribute) ?: error("Player doesn't have attribute $attribute")
        val modifier = attributeInstance.modifiers.find { modifier ->
            modifier.uniqueId == uuid && modifier.name == name // Quick check
        } ?: error("Player doesn't have modifier $this")

        attributeInstance.removeModifier(modifier)
    }
}
