package dev.racci.terix.core.origins

import dev.racci.minix.api.extensions.parse
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.AbstractOrigin
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material

class MerlingOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name = "Merling"
    override val colour = NamedTextColor.AQUA!!
    override val hurtSound = Key.key("entity.salmon.hurt")
    override val deathSound = Key.key("entity.salmon.death")

    override suspend fun onRegister() {
        item {
            named(displayName)
            material(Material.TRIDENT)
            lore {
                this[0] = "<aqua>A mysterious origin.".parse()
                this[1] = "<aqua>It's not clear what it is.".parse()
            }
        }
    }
}
