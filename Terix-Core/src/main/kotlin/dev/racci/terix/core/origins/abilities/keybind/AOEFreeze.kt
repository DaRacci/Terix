package dev.racci.terix.core.origins.abilities.keybind

import com.destroystokyo.paper.ParticleBuilder
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.dsl.dslMutator
import dev.racci.terix.api.origins.abilities.keybind.TriggeringKeybindAbility
import dev.racci.terix.api.origins.origin.Origin
import kotlinx.coroutines.delay
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration

public class AOEFreeze(
    override val abilityPlayer: Player,
    override val linkedOrigin: Origin,
    override val cooldownDuration: Duration,
    public val freezeDuration: Duration,
    public val radius: Double
) : TriggeringKeybindAbility() {
    override suspend fun handleTrigger() {
        val entities = deferredSync { abilityPlayer.world.getNearbyLivingEntities(abilityPlayer.location, radius) }.await()
            .filterNot { it === abilityPlayer }
            .ifEmpty { return failActivation() }

        val potion = dslMutator<PotionEffectBuilder> {
            type = PotionEffectType.SLOW
            duration = freezeDuration
            amplifier = 10
            ambient = false
            particles = false
            icon = false
        }.asNew().tagged()

        val offset = 0.5
        val points = 360
        val step = 1
        for (i in 0 until points) {
            val dx = cos(step + Math.PI * 2 * (i.toDouble() / points))
            val dz = sin(step + Math.PI * 2 * (i.toDouble() / points))
            val angle = atan2(dz, dx)
            val xAng = cos(angle)
            val zAng = sin(angle)

            ParticleBuilder(Particle.SNOWFLAKE)
                .source(abilityPlayer)
                .location(abilityPlayer.location.clone().add(0.0, offset, 0.0))
                .offset(xAng, 0.0, zAng)
                .spawn()
        }

        sync {
            entities.forEach { entity ->
                entity.addPotionEffect(potion)
                entity.lockFreezeTicks(true)
                entity.freezeTicks = entity.maxFreezeTicks
            }
        }

        delay(freezeDuration)

        entities.filterIsInstance<LivingEntity>()
            .forEach { it.lockFreezeTicks(false) }
    }
}
