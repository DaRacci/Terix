package dev.racci.terix.api.dsl

import dev.racci.minix.api.extensions.inWholeTicks
import dev.racci.minix.api.extensions.ticks
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.Trigger
import org.bukkit.NamespacedKey
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.time.Duration

class PotionEffectBuilder() {

    constructor(builder: PotionEffectBuilder.() -> Unit) : this() {
        builder(this)
    }

    var amplifier: Int = 1
    var duration: Duration = 20.ticks
    var durationInt: Int? = null
    var type: PotionEffectType? = null
    var ambient: Boolean = false
    var particles: Boolean? = null
    var icon: Boolean? = null
    var key: NamespacedKey? = null

    fun originKey(origin: AbstractOrigin, trigger: Trigger) {
        this.key = NamespacedKey("terix", "origin_potion_${origin.name.lowercase()}_${trigger.name.lowercase()}")
    }

    fun originKey(origin: String, trigger: String) {
        this.key = NamespacedKey("terix", "origin_potion_${origin.lowercase()}_${trigger.lowercase()}")
    }

    fun build(): PotionEffect =
        PotionEffect(
            type ?: error("Type must be set for potion builder."),
            durationInt ?: duration.inWholeTicks.toInt(),
            amplifier,
            ambient,
            particles ?: !ambient,
            icon ?: particles ?: !ambient,
            key.takeUnless { it == null || !it.toString().matches(regex) } ?: error("Invalid key. Was null or didn't match terix:origin_potion_(?<origin>[a-z0-9/_.-]+)_(?<trigger>[a-z0-9/_.-]+): $key")
        )

    companion object {
        val regex by lazy { Regex("terix:origin_potion_(?<origin>[a-z0-9/_.-]+)_(?<trigger>[a-z0-9/_.-]+)") }
    }
}
