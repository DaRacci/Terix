package dev.racci.terix.core.origins

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
            material = Material.SNOWBALL
            lore = """
                <gold>A magical snowball that will 
                <green>freeze</green> any player that
                <red>touches it
            """.trimIndent()
        }
    }
}
