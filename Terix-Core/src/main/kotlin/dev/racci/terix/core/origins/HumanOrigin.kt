package dev.racci.terix.core.origins

import dev.racci.minix.api.builders.ItemBuilderDSL
import dev.racci.minix.api.extensions.parse
import dev.racci.minix.api.plugin.MinixPlugin
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.Trigger
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.potion.PotionEffectType

class HumanOrigin(plugin: MinixPlugin) : AbstractOrigin(plugin) {

    override val name = "Human"
    override val colour: NamedTextColor = NamedTextColor.GRAY
    override val hurtSound = Key.key("minecraft", "entity.player.hurt")
    override val deathSound = Key.key("minecraft", "entity.player.death")
    override val guiItem by lazy {
        ItemBuilderDSL.from(Material.APPLE) {
            name = displayName
        }
    }

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
    }
}
