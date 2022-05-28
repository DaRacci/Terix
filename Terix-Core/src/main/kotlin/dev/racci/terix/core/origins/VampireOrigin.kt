package dev.racci.terix.core.origins

import dev.racci.minix.api.extensions.parse
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.api.origins.sounds.SoundEffect
import kotlinx.datetime.Instant
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.potion.PotionEffectType
import kotlin.time.Duration

class VampireOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name = "Vampire"
    override val colour = NamedTextColor.DARK_RED!!

    override val nightVision = true

    override suspend fun onRegister() {
        sounds.hurtSound = SoundEffect("entity.bat.hurt")
        sounds.deathSound = SoundEffect("entity.bat.death")
        sounds.ambientSound = SoundEffect("entity.bat.ambient")

        damage {
            Trigger.SUNLIGHT += 100.0
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
}
