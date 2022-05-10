package dev.racci.terix.core.origins

import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.AbstractOrigin
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material

class FairyOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name = "Fairy"
    override val colour = NamedTextColor.YELLOW!!
    override val hurtSound = Key.key("entity.blaze.hurt")
    override val deathSound = Key.key("entity.blaze.death")

    override suspend fun onRegister() {
        item {
            material = Material.GLOWSTONE_DUST
            lore = "<yellow>A magical dust that can be used to create a fairy."
        }
    }
}
