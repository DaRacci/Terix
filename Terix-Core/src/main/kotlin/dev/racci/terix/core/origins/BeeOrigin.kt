package dev.racci.terix.core.origins

import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.AbstractOrigin
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material

class BeeOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name = "Bee"
    override val colour = NamedTextColor.GOLD!!
    override val hurtSound = Key.key("entity.bee.hurt")
    override val deathSound = Key.key("entity.bee.death")

    override suspend fun onRegister() {
        item {
            material = Material.HONEYCOMB
            lore = """
                <gold>A bee is a type of bee.
                <gold>It is a type of bee.
            """.trimIndent()
        }
    }
}
