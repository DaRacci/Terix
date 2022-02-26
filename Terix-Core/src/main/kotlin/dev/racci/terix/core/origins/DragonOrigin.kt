package dev.racci.terix.core.origins

import dev.racci.minix.api.extensions.parse
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.AbstractOrigin
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material

class DragonOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name by lazy { "Dragon" }
    override val colour by lazy { NamedTextColor.LIGHT_PURPLE!! }
    override val hurtSound by lazy { Key.key("entity.enderman.hurt") }
    override val deathSound by lazy { Key.key("entity.enderman.death") }

    override suspend fun onRegister() {
        item {
            named(displayName)
            material(Material.DRAGON_BREATH)
            lore {
                this[0] = "<light_purple>A breath of fire that can be used to summon a dragon.".parse()
            }
        }
    }
}
