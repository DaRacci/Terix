package dev.racci.terix.api.origins

import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import java.util.UUID

class AttributeModification() {

    constructor(
        uuid: UUID,
        value: Double,
        attribute: Attribute,
        operation: ModificationType = ModificationType.ADD
    ) : this() {
        this.uuid = uuid
        this.value = value
        this.attribute = attribute
        this.operation = operation
    }

    private var uuid: UUID? = null
    var value: Double? = null
    var attribute: Attribute? = null
    var operation: ModificationType? = null

    @Throws(AssertionError::class)
    fun invokeOnto(player: Player) {
        if (uuid == null) uuid = UUID.randomUUID()
        assert(value != null) { "Value cannot be null" }
        assert(attribute != null) { "Attribute cannot be null" }
        assert(operation != null) { "Operation cannot be null" }
        val attributeInstance = player.getAttribute(attribute!!) ?: error("The attribute must already exist on the player.")

        val modificationNeeded = when (operation!!) {
            ModificationType.ADD -> attributeInstance.value + value!!
            ModificationType.MULTIPLY -> attributeInstance.value * value!!
            ModificationType.SUBTRACT -> attributeInstance.value - value!!
        }
    }

    fun removeFrom(player: Player) {
    }

    enum class ModificationType { ADD, SUBTRACT, MULTIPLY }
}
