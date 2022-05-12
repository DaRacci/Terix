package dev.racci.terix.api.dsl

import dev.racci.minix.api.extensions.scheduler
import dev.racci.minix.api.utils.getKoin
import dev.racci.terix.api.Terix
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import java.util.UUID
import kotlin.properties.Delegates
import kotlin.time.Duration

// TODO: Test but like this requires being able to use the scheduler so i can't test it
class TimedAttributeBuilder() {

    constructor(builder: TimedAttributeBuilder.() -> Unit) : this() {
        builder(this)
    }

    var uuid: UUID? = null
    var attribute by Delegates.notNull<Attribute>()
    var name by Delegates.notNull<String>()
    var duration by Delegates.notNull<Duration>()
    var amount by Delegates.notNull<Double>()
    var operation by Delegates.notNull<AttributeModifier.Operation>()

    fun invoke(player: Player) {
        val modifier = AttributeModifier(uuid ?: UUID.randomUUID(), name, amount, operation)
        player.getAttribute(attribute)?.addModifier(modifier)
        scheduler {
            player.getAttribute(attribute)?.removeModifier(modifier)
        }.runAsyncTaskLater(getKoin().get<Terix>(), duration)
    }
}
