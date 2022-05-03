package dev.racci.terix.core.origins

import dev.racci.minix.api.extensions.parse
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.AbstractOrigin
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material

class BlizzOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name = "Blizz"
    override val colour = NamedTextColor.GOLD!!
    override val hurtSound = Key.key("minecraft", "entity.snowgolem.hurt")
    override val deathSound = Key.key("minecraft", "entity.snowgolem.death")

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
