package dev.racci.terix.api.dsl

import dev.racci.minix.api.extensions.scheduler
import dev.racci.minix.api.utils.getKoin
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.origin.OriginValues
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import java.util.UUID
import kotlin.time.Duration

// TODO: Test but like this requires being able to use the scheduler so i can't test it
// TODO -> Account for changes while activated.
public class TimedAttributeBuilder(
    uuid: UUID = UUID.randomUUID(),
    attribute: Attribute? = null,
    name: String? = null,
    amount: Double? = null,
    duration: Duration? = null,
    operation: AttributeModifier.Operation? = null
) : CachingBuilder<AttributeModifier>() {

    public var uuid: UUID by createWatcher(uuid)
    public var attribute: Attribute by createWatcher(attribute)
    public var name: String by createWatcher(name)
    public var amount: Double by createWatcher(amount)
    public var duration: Duration by createWatcher(duration)
    public var operation: AttributeModifier.Operation by createWatcher(operation)

    public fun materialName(
        material: Material,
        origin: OriginValues
    ): TimedAttributeBuilder {
        name = StringBuilder("terix:timed_attribute_")
            .append(origin.name.lowercase())
            .append("/")
            .append(material.name.lowercase())
            .toString()
        return this
    }

    public operator fun invoke(player: Player) {
        player.getAttribute(attribute)?.addModifier(get())

        scheduler {
            player.getAttribute(attribute)?.removeModifier(get()) // TODO -> If this changes while activated, it will not be removed.
        }.runAsyncTaskLater(getKoin().get<Terix>(), duration)
    }

    override fun create(): AttributeModifier = AttributeModifier(uuid, name, amount, operation)
}
