package dev.racci.terix.core.origins

import dev.racci.minix.api.extensions.parse
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.Trigger
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.potion.PotionEffectType

class HumanOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name by lazy { "Human" }
    override val colour by lazy { NamedTextColor.GRAY!! }
    override val hurtSound by lazy { Key.key("minecraft", "entity.player.hurt") }
    override val deathSound by lazy { Key.key("minecraft", "entity.player.death") }

    override suspend fun onRegister() {
        potions {
            Trigger.ON causes {
                type = PotionEffectType.DAMAGE_RESISTANCE
                durationInt = Int.MAX_VALUE
                amplifier = 1
                ambient = true
            }
        }
        title {
            Trigger.DAY causes {
                title = "<blue>Day".parse()
                subtitle = "<aqua>haha its bright blue now".parse()
            }
        }
        item {
            named(displayName)
            material(Material.APPLE)
            lore {
                this[0] = "<gray>This is a human origin".parse()
            }
        }
    }
}
