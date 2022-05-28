package dev.racci.terix.core.origins

import dev.racci.minix.api.events.PlayerShiftRightClickEvent
import dev.racci.minix.api.extensions.asBoolean
import dev.racci.minix.api.extensions.parse
import dev.racci.minix.api.utils.now
import dev.racci.minix.api.utils.safeCast
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.api.origins.sounds.SoundEffect
import kotlinx.datetime.Instant
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.world.damagesource.DamageSource
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType
import xyz.xenondevs.particle.ParticleBuilder
import xyz.xenondevs.particle.ParticleEffect
import java.awt.Color
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class VampireOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name = "Vampire"
    override val colour = NamedTextColor.DARK_RED!!

    override val nightVision = true

    override suspend fun onRegister() {
        sounds.hurtSound = SoundEffect("entity.bat.hurt")
        sounds.deathSound = SoundEffect("entity.bat.death")
        sounds.ambientSound = SoundEffect("entity.bat.ambient")

        damage {
            Trigger.SUNLIGHT += 160.0
        }
        title {
            Trigger.SUNLIGHT += {
                title = "<red>You feel week.".parse()
                subtitle = "<red>Return to the dark to regain your strength.".parse()
                sound = Key.key("minecraft", "entity.bat.hurt")
            }
            Trigger.DARKNESS += {
                title = "<green>You feel stronger.".parse()
                subtitle = "<green>You feel the power of the sun.".parse()
                sound = Key.key("minecraft", "item.shield.break")
            }
        }
        potions {
            Trigger.SUNLIGHT += {
                type = PotionEffectType.WEAKNESS
                duration = Duration.INFINITE
                ambient = true
            }
            Trigger.DARKNESS += {
                type = PotionEffectType.INCREASE_DAMAGE
                duration = Duration.INFINITE
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

    private val cooldownList = object : HashSet<Player>() {
        val cooldowns = mutableMapOf<Player, Instant>()

        override fun add(element: Player): Boolean {
            cooldowns += element to now()
            return super.add(element)
        }

        override fun remove(element: Player): Boolean {
            cooldowns -= element
            return super.remove(element)
        }

        override fun contains(element: Player): Boolean {
            if (cooldowns[element]?.let { it + 1.seconds }?.compareTo(now())?.asBoolean() == true) {
                remove(element)
                return false
            }
            return super.contains(element)
        }
    }

    // TODO: Sucking sound
    override suspend fun onSneakRightClick(event: PlayerShiftRightClickEvent) {
        if (event.player in cooldownList) return
        val entity = event.entity as? LivingEntity ?: return

        val amountTaken = (event.player.maxHealth - event.player.health).coerceAtMost(1.0)
        val vampAmount = (entity.maxHealth / 8).coerceAtMost(entity.health.coerceAtMost(amountTaken * 2))
        val killing = entity.maxHealth - vampAmount <= 0.0

        plugin.log.debug {
            """\n
                |Vampire: ${event.player.name}
                |Entity: ${entity.name}
                |Amount Taken: $amountTaken
                |Vampire Amount: $vampAmount
                |Killing: $killing
            """.trimIndent()
        }

        event.player.health += vampAmount
        event.player.sendHealthUpdate()
        ParticleBuilder(ParticleEffect.FALLING_DUST)
            .setColor(Color.RED)
            .setAmount(6)
            .setLocation(entity.location)
            .display()

        if (killing) {
            entity.killer = event.player
            entity.toNMS().health = 0.0f
            return entity.toNMS().die(DamageSource.playerAttack(event.player.toNMS()))
        }

        entity.health = (entity.health - vampAmount)
        entity.safeCast<Player>()?.sendHealthUpdate()
        cooldownList += event.player
    }
}
