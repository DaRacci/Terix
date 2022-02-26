package dev.racci.terix.core.origins

import dev.racci.minix.api.extensions.parse
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.AbstractOrigin
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material

class BeeOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name by lazy { "Bee" }
    override val colour by lazy { NamedTextColor.GOLD!! }
    override val hurtSound by lazy { Key.key("entity.bee.hurt") }
    override val deathSound by lazy { Key.key("entity.bee.death") }

    override suspend fun onRegister() {
        item {
            named(displayName)
            material(Material.HONEYCOMB)
            lore {
                this[0] = "<gold>A bee is a type of Animal".parse()
                this[1] = "<gold>and is the only type of bee that can fly.".parse()
            }
        }
    }
}
