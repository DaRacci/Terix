package dev.racci.terix.api.dsl

import dev.racci.minix.api.extensions.inWholeTicks
import org.bukkit.NamespacedKey
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.properties.Delegates
import kotlin.time.Duration

class PotionEffectBuilder() {

    constructor(builder: PotionEffectBuilder.() -> Unit) : this() {
        builder()
    }

    var amplifier by Delegates.notNull<Int>()
    var duration by Delegates.notNull<Duration>()
    var durationInt: Int? = null
    var type by Delegates.notNull<PotionEffectType>()
    var ambient: Boolean? = null
    var particles: Boolean? = null
    var icon: Boolean? = null
    var key: NamespacedKey? = null

    fun originKey(origin: String) {
        key = NamespacedKey("origin", origin.lowercase())
    }

    fun build(): PotionEffect =
        PotionEffect(
            type,
            durationInt ?: duration.inWholeTicks.toInt(),
            amplifier,
            ambient ?: false,
            particles ?: (ambient == false),
            (icon ?: particles) == true,
            key
        )
}
