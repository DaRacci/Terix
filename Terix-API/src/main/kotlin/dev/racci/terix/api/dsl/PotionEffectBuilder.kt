package dev.racci.terix.api.dsl

import dev.racci.minix.api.extensions.inWholeTicks
import dev.racci.minix.api.extensions.ticks
import dev.racci.terix.api.data.OriginNamespacedTag
import org.bukkit.entity.LivingEntity
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.time.Duration

public class PotionEffectBuilder constructor(
    amplifier: Int? = null,
    duration: Duration? = null,
    type: PotionEffectType? = null,
    ambient: Boolean? = false,
    particles: Boolean? = null,
    icon: Boolean? = null,
    tag: OriginNamespacedTag? = null
) : CachingBuilder<PotionEffect>() {

    public var amplifier: Int by createWatcher(amplifier ?: 1)
    public var duration: Duration by createWatcher(duration ?: 20.ticks)
    public var type: PotionEffectType by createWatcher(type)
    public var ambient: Boolean by createWatcher(ambient ?: false)
    public var particles: Boolean by createWatcher(particles ?: ambient)
    public var icon: Boolean by createWatcher(icon)
    public var tag: OriginNamespacedTag by createLockingWatcher(tag)

    override fun create(): PotionEffect = PotionEffect(
        ::type.watcherOrNull() ?: error("Type must be set for potion builder."),
        duration.inWholeTicks.coerceAtMost(Integer.MAX_VALUE.toLong()).toInt(),
        amplifier,
        ambient,
        ::particles.watcherOrNull() ?: !ambient,
        ::icon.watcherOrNull() ?: ::particles.watcherOrNull() ?: !ambient,
        ::tag.watcherOrNull()?.bukkitKey
    )

    public operator fun invoke(entity: LivingEntity) {
        entity.addPotionEffect(get())
    }
}
