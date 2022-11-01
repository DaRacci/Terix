package dev.racci.terix.api.origins.abilities

import dev.racci.minix.nms.aliases.toNMS
import net.minecraft.world.phys.Vec3
import org.bukkit.entity.Player
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public class DragonBreath : Ability(AbilityType.TRIGGER) {
    override val cooldown: Duration = 10.seconds

    // TODO -> Less NMS bullshitery
    override suspend fun onActivate(player: Player) {
        val targetLoc = player.getTargetBlock(50) ?: return failActivation(player)
        val vec3 = Vec3(targetLoc.x - player.location.x, 0.0, targetLoc.z - player.location.z).normalize()
        val vec32 = Vec3(sin(player.location.pitch * (sin(PI.toFloat() / 180f)).toDouble()), 0.0, (-cos(player.location.pitch * (PI.toFloat() / 180f)).toDouble())).normalize() // WHAT?? (Extracted from ender dragon code)
        val l = vec32.dot(vec3).toFloat()
        var m = acos(l.toDouble() * (180f / PI.toFloat()).toDouble()).toFloat() // WHAT?? (Extracted from ender dragon code)
        m += 0.5f

        val nms = player.toNMS()
        val vec33: Vec3 = nms.getViewVector(1.0f)
        val o: Double = nms.eyePosition.x - vec33.x * 1.0
        val p: Double = nms.getY(0.5) + 0.5
        val q: Double = nms.eyePosition.z - vec33.z * 1.0
        val r: Double = nms.x - o
        val s: Double = nms.getY(0.5) - p
        val t: Double = nms.z - q

        nms.level.levelEvent(null as net.minecraft.world.entity.player.Player?, 1017, nms.blockPosition(), 0) // Sound i think?

        val dragonFireball = net.minecraft.world.entity.projectile.DragonFireball(nms.level, nms, r, s, t)
        dragonFireball.moveTo(o, p, q, 0.0f, 0.0f)

        sync { nms.level.addFreshEntity(dragonFireball) }
    }
}
