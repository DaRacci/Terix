package dev.racci.terix.api.origins.abilities.keybind

import com.destroystokyo.paper.event.entity.EnderDragonFireballHitEvent
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.minix.api.extensions.reflection.withCast
import dev.racci.terix.api.data.player.TerixPlayer
import dev.racci.terix.api.extensions.fold
import dev.racci.terix.api.origins.origin.Origin
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile
import net.minecraft.world.level.Level
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.bukkit.event.entity.CreatureSpawnEvent
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public class DragonBreath(
    override val abilityPlayer: TerixPlayer,
    override val linkedOrigin: Origin,
    override val cooldownDuration: Duration = 10.seconds
) : TriggeringKeybindAbility() {

    // TODO -> Less NMS bullshitery
    override suspend fun handleTrigger() {
        val targetLoc = abilityPlayer.getTargetBlock(50) ?: return failActivation()
        val vec3 = Vec3(targetLoc.x - abilityPlayer.location.x, 0.0, targetLoc.z - abilityPlayer.location.z).normalize()
        val vec32 = Vec3(sin(abilityPlayer.location.pitch * (sin(PI.toFloat() / 180f)).toDouble()), 0.0, (-cos(abilityPlayer.location.pitch * (PI.toFloat() / 180f)).toDouble())).normalize() // WHAT?? (Extracted from ender dragon code)
        val l = vec32.dot(vec3).toFloat()
        var m = acos(l.toDouble() * (180f / PI.toFloat()).toDouble()).toFloat() // WHAT?? (Extracted from ender dragon code)
        m += 0.5f

        val nms = abilityPlayer.handle
        val vec33: Vec3 = nms.getViewVector(1.0f)
        val o: Double = nms.eyePosition.x - vec33.x * 1.0
        val p: Double = nms.getY(0.5) + 0.5
        val q: Double = nms.eyePosition.z - vec33.z * 1.0
        val r: Double = nms.x - o
        val s: Double = nms.getY(0.5) - p
        val t: Double = nms.z - q

        nms.level.levelEvent(null as net.minecraft.world.entity.player.Player?, 1017, nms.blockPosition(), 0) // Sound i think?

        val dragonFireball = DragonFireball(nms.level, nms, r, s, t)
        dragonFireball.moveTo(o, p, q, 0.0f, 0.0f)

        sync { nms.level.addFreshEntity(dragonFireball) }
    }

    private class DragonFireball(
        world: Level,
        owner: LivingEntity,
        directionX: Double,
        directionY: Double,
        directionZ: Double
    ) : AbstractHurtingProjectile(
        EntityType.DRAGON_FIREBALL,
        owner,
        directionX,
        directionY,
        directionZ,
        world
    ) {

        override fun onHit(hitResult: HitResult) {
            super.onHit(hitResult)
            if (hitResult.type == HitResult.Type.ENTITY && ownedBy((hitResult as EntityHitResult).entity)) return

            val entitiesInside = level.getEntitiesOfClass(LivingEntity::class.java, this.boundingBox.inflate(4.0, 2.0, 4.0))
            val areaEffectCloud = net.minecraft.world.entity.AreaEffectCloud(level, this.x, this.y, this.z).apply {
                this@DragonFireball.owner.withCast<LivingEntity, Unit> { this@apply.owner = this }
                this.particle = ParticleTypes.DRAGON_BREATH
                this.radius = 3.0f
                this.duration = 600
                this.setRadiusPerTick((7.0f - this.radius) / this.duration.toFloat())
                this.addEffect(MobEffectInstance(MobEffects.HARM, 1, 1))
            }

            if (entitiesInside.isNotEmpty()) {
                for (livingEntity in entitiesInside) {
                    val distance = this.distanceToSqr(livingEntity)
                    if (distance >= 16.0) continue
                    areaEffectCloud.setPos(livingEntity.x, livingEntity.y, livingEntity.z)
                    break
                }
            }

            EnderDragonFireballHitEvent(
                bukkitEntity.castOrThrow(),
                entitiesInside.map(LivingEntity::getBukkitLivingEntity),
                areaEffectCloud.bukkitEntity.castOrThrow()
            ).fold(
                onFailure = { areaEffectCloud.discard() },
                onSuccess = {
                    level.levelEvent(2006, blockPosition(), if (this.isSilent) -1 else 1)
                    level.addFreshEntity(areaEffectCloud, CreatureSpawnEvent.SpawnReason.EXPLOSION)
                }
            )
            discard()
        }

        override fun isPickable(): Boolean {
            return false
        }

        override fun hurt(source: DamageSource, amount: Float): Boolean {
            return false
        }

        override fun getTrailParticle(): ParticleOptions {
            return ParticleTypes.DRAGON_BREATH
        }

        override fun shouldBurn(): Boolean {
            return false
        }
    }
}
