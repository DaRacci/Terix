package dev.racci.terix.core.origins

import dev.racci.minix.api.extensions.parse
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.Trigger
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.potion.PotionEffectType
import kotlin.time.Duration

class VampireOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name = "Vampire"
    override val colour = NamedTextColor.DARK_RED!!
    override val hurtSound = Key.key("minecraft", "entity.bat.hurt")
    override val deathSound = Key.key("minecraft", "entity.bat.death")

    override val nightVision = true

    override suspend fun onRegister() {
        damage {
            Trigger.SUNLIGHT ticks 100.0
        }
        title {
            Trigger.SUNLIGHT causes {
                title = "<red>You feel week.".parse()
                subtitle = "<red>Return to the dark to regain your strength.".parse()
                sound = Key.key("minecraft", "entity.bat.hurt")
            }
        }
        potions {
            Trigger.SUNLIGHT causes {
                type = PotionEffectType.WEAKNESS
                duration = Duration.INFINITE
                ambient = true
                particles = false
                icon = false
            }
        }
        item {
            named(displayName)
            material(Material.BEETROOT_SOUP)
            lore {
                this[0] = "<dark_red>The Vampire is a type of vampire that is able to turn into a bat.".parse()
                this[1] = "<dark_red>This is a powerful ability that can be used to kill any player.".parse()
            }
        }
    }
}
