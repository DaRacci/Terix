package dev.racci.terix.core.origins

import dev.racci.minix.api.events.PlayerShiftRightClickEvent
import dev.racci.minix.api.extensions.parse
import dev.racci.minix.api.extensions.playSound
import dev.racci.minix.api.extensions.sync
import dev.racci.minix.api.utils.now
import dev.racci.minix.api.utils.safeCast
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.sounds.SoundEffect
import dev.racci.terix.api.origins.states.State
import kotlinx.datetime.Instant
import net.kyori.adventure.text.format.TextColor
import net.minecraft.world.damagesource.DamageSource
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType
import xyz.xenondevs.particle.ParticleBuilder
import xyz.xenondevs.particle.ParticleEffect
import java.awt.Color
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.seconds

// TODO -> Turn into bat via turning them invisible and adding then onto a bat.
// TODO -> Raw meat gives the amount of cooked meat.
// TODO -> Cooked meat gives the amount of raw meat.
// TODO -> Rotten flesh no hunger, Spider eye no poison and higher saturation.
// TODO -> All food sources that dont supply blood should be reduced to 0.5 saturation or close to.
// TODO -> Cake good.
// TODO -> Immune to wither-rose.
// TODO -> Take damage to healing potion, heal from damage potions like zombies.
class VampireOrigin(override val plugin: Terix) : Origin() {

    override val name = "Vampire"
    override val colour = TextColor.fromHexString("#ff1234")!!

    override suspend fun onRegister() {
        sounds.hurtSound = SoundEffect("entity.bat.hurt")
        sounds.deathSound = SoundEffect("entity.bat.death")
        sounds.ambientSound = SoundEffect("entity.bat.ambient")

        potions {
            listOf(State.TimeState.NIGHT, State.WorldState.NETHER) += {
                type = PotionEffectType.NIGHT_VISION
                duration = INFINITE
                amplifier = 0
                ambient = true
            }
        }
        damage {
            State.LightState.SUNLIGHT += 160.0
        }
        title {
            State.LightState.SUNLIGHT += {
                subtitle = "<red>Return to the dark to regain your strength.".parse()
            }
        }
        potions {
            State.LightState.SUNLIGHT += {
                type = PotionEffectType.WEAKNESS
                duration = INFINITE
                ambient = true
            }
            State.LightState.DARKNESS += {
                type = PotionEffectType.INCREASE_DAMAGE
                duration = INFINITE
                ambient = true
            }
        }
        item {
            material = Material.BEETROOT_SOUP
            lore = """
                <dark_red>The Vampire is a type of vampire that is able to turn into a bat.
                <dark_red>This is a powerful ability that can be used to kill any player.
            """.trimIndent()
        }
    }

    private val cooldowns = mutableMapOf<Player, Instant>()

    // TODO: Sucking sound
    override suspend fun onSneakRightClick(event: PlayerShiftRightClickEvent) {
        val now = now()
        if (event.player in cooldowns) {
            val inst = cooldowns[event.player]!!

            if (inst + 1.seconds > now) return
            cooldowns -= event.player
        }

        val entity = event.entity as? LivingEntity ?: return

        val amountTaken = (event.player.maxHealth - event.player.health).coerceAtMost(2.0)
        val vampAmount = (entity.maxHealth / 8).coerceAtMost(entity.health.coerceAtMost(amountTaken))
        val killing = entity.maxHealth - vampAmount <= 0.0

        event.player.health += vampAmount
        event.player.sendHealthUpdate()
        ParticleBuilder(ParticleEffect.FALLING_DUST)
            .setColor(Color.RED)
            .setAmount(6)
            .setLocation(entity.location)
            .display()

        if (killing) {
            val nmsPlayer = event.player.toNMS()

            entity.killer = event.player
            nmsPlayer.health = 0.0f

            sync { nmsPlayer.die(DamageSource.playerAttack(event.player.toNMS())) }
            return
        }

        entity.location.playSound(Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 1.0f)
        entity.health = (entity.health - vampAmount)
        entity.safeCast<Player>()?.sendHealthUpdate()
        cooldowns[event.player] = now
    }
}
