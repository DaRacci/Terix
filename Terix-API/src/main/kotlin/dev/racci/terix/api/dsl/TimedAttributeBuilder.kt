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
class TimedAttributeBuilder(
    uuid: UUID = UUID.randomUUID(),
    attribute: Attribute? = null,
    name: String? = null,
    amount: Double? = null,
    duration: Duration? = null,
    operation: AttributeModifier.Operation? = null
) : CachingBuilder<AttributeModifier>() {

    var uuid by createWatcher(uuid)
    var attribute by createWatcher(attribute)
    var name by createWatcher(name)
    var amount by createWatcher(amount)
    var duration by createWatcher(duration)
    var operation by createWatcher(operation)

    fun materialName(
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

    operator fun invoke(player: Player) {
        player.getAttribute(attribute)?.addModifier(get())

        scheduler {
            player.getAttribute(attribute)?.removeModifier(get()) // TODO -> If this changes while activated, it will not be removed.
        }.runAsyncTaskLater(getKoin().get<Terix>(), duration)
    }

    override fun create() = AttributeModifier(uuid, name, amount, operation)
}
