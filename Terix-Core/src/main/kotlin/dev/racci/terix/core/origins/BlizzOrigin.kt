package dev.racci.terix.core.origins

import dev.racci.minix.api.extensions.parse
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.AbstractOrigin
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material

class BlizzOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name by lazy { "Blizz" }

    override val colour by lazy { NamedTextColor.GOLD!! }

    override val hurtSound by lazy { Key.key("minecraft", "entity.snowgolem.hurt") } // TODO: Look at

    override val deathSound by lazy { Key.key("minecraft", "entity.snowgolem.death") } // TODO: Look at

    override suspend fun onRegister() {
        item {
            named(displayName)
            material(Material.SNOWBALL)
            lore {
                this[0] = "<gold>A magical snowball that can be thrown at enemies.".parse()
                this[1] = "<gold>It will freeze enemies in place.".parse()
            }
        }
    }
}
