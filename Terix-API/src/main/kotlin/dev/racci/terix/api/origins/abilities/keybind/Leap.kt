package dev.racci.terix.api.origins.abilities.keybind

import com.destroystokyo.paper.ParticleBuilder
import dev.racci.terix.api.data.player.TerixPlayer
import dev.racci.terix.api.origins.abilities.RayCastingSupplier
import dev.racci.terix.api.origins.origin.Origin
import io.ktor.utils.io.bits.reverseByteOrder
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.Particle
import kotlin.time.Duration

public class Leap(
    override val abilityPlayer: TerixPlayer,
    override val linkedOrigin: Origin,
    override val cooldownDuration: Duration,
    override val chargeTime: Duration,
    public val jumpHeight: Double = 3.0
) : HoldingChargeKeybindAbility() {

    override suspend fun handleChargeStart() {
        abilityPlayer.playSound(abilityPlayer, org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1F, 1F)
    }

    override suspend fun handleChargeFull() {
        abilityPlayer.playSound(Sound.sound(Key.key("minecraft:entity.ender_dragon.flap"), Sound.Source.PLAYER, 1f, 1f))
    }

    override suspend fun handleChargeRelease(charge: Float) {
        RayCastingSupplier.of(abilityPlayer)
            .filter { (abilityPlayer.location.y - it.hitPosition.y) <= jumpHeight }
            .fold(
                ifEmpty = ::failActivation,
                ifSome = {
                    abilityPlayer.playSound(Sound.sound(Key.key("minecraft:entity.slime.jump"), Sound.Source.PLAYER, 1f, charge.reverseByteOrder())) // TODO -> Pitch based on charge
                    abilityPlayer.velocity = abilityPlayer.location.direction.multiply(jumpHeight * charge)
                    ParticleBuilder(Particle.CLOUD)
                        .offset(0.5, 0.5, 0.5)
                        .source(abilityPlayer.backingPlayer)
                        .location(abilityPlayer.location)
                        .count(5)
                        .also { builder -> sync { builder.receivers(16) } }
                        .spawn()
                }
            )
    }

    override suspend fun handleChargeIncrease(charge: Float) {
        ParticleBuilder(Particle.SLIME)
            .offset(0.5, 0.5, 0.5)
            .source(abilityPlayer.backingPlayer)
            .location(abilityPlayer.location)
            .count(10 * charge.toInt())
            .also { builder -> sync { builder.receivers(16) } }
            .spawn()
    }

    override suspend fun handleChargeDecrease(charge: Float) {
        ParticleBuilder(Particle.ASH)
            .offset(0.5, 0.5, 0.5)
            .source(abilityPlayer.backingPlayer)
            .location(abilityPlayer.location)
            .count(10 * charge.toInt())
            .also { builder -> sync { builder.receivers(16) } }
            .spawn()
    }
}
