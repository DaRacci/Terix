package dev.racci.terix.core.origins.abilities.keybind

import com.destroystokyo.paper.ParticleBuilder
import dev.racci.terix.api.data.player.TerixPlayer
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.dsl.dslMutator
import dev.racci.terix.api.origins.abilities.keybind.TriggeringKeybindAbility
import dev.racci.terix.api.origins.origin.Origin
import kotlinx.coroutines.delay
import org.bukkit.Particle
import org.bukkit.entity.LivingEntity
import org.bukkit.potion.PotionEffectType
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration

public class AOEFreeze(
    override val abilityPlayer: TerixPlayer,
    override val linkedOrigin: Origin,
    override val cooldownDuration: Duration,
    public val freezeDuration: Duration,
    public val radius: Double
) : TriggeringKeybindAbility() {
    private val slowPotion = SLOW.on(PotionEffectBuilder(duration = freezeDuration)).applyTag()

    override suspend fun handleTrigger() {
        val entities = deferredSync { abilityPlayer.world.getNearbyLivingEntities(abilityPlayer.location, radius) }.await()
            .filterNot { it === abilityPlayer }
            .ifEmpty { return failActivation() }

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
                slowPotion(entity)
                entity.lockFreezeTicks(true)
                entity.freezeTicks = entity.maxFreezeTicks
            }
        }

        delay(freezeDuration)

        entities.filterIsInstance<LivingEntity>()
            .forEach { it.lockFreezeTicks(false) }
    }

    private companion object {
        val SLOW = dslMutator<PotionEffectBuilder> {
            type = PotionEffectType.SLOW
            amplifier = 10
            ambient = false
            particles = false
            icon = false
        }
    }
}
