package dev.racci.terix.core.origins

import dev.racci.minix.api.extensions.parse
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.AbstractOrigin
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material

class FairyOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name by lazy { "Fairy" }
    override val colour by lazy { NamedTextColor.YELLOW!! }
    override val hurtSound by lazy { Key.key("entity.blaze.hurt") }
    override val deathSound by lazy { Key.key("entity.blaze.death") }

    override suspend fun onRegister() {
        item {
            named(displayName)
            material(Material.GLOWSTONE_DUST)
            lore {
                this[0] = "<yellow>A magical creature that can be summoned to fight for you.".parse()
            }
        }
    }
}
