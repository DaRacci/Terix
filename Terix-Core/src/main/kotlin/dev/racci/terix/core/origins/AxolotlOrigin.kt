package dev.racci.terix.core.origins

import dev.racci.minix.api.extensions.parse
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.AbstractOrigin
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material

class AxolotlOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name = "Axolotl"
    override val colour = NamedTextColor.WHITE!!
    override val hurtSound = Key.key("entity.axolotl.hurt")
    override val deathSound = Key.key("entity.axolotl.death")

    override val waterBreathing by lazy { true }

    override suspend fun onRegister() {
        item {
            named(displayName)
            material(Material.AXOLOTL_BUCKET)
            lore {
                this[0] = "<white>Axolotl is a creature.".parse()
            }
        }
    }
}
