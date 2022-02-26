package dev.racci.terix.core.origins

import dev.racci.minix.api.extensions.parse
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.AbstractOrigin
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material

class AxolotlOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name by lazy { "Axolotl" }
    override val colour by lazy { NamedTextColor.WHITE!! }
    override val hurtSound by lazy { Key.key("entity.axolotl.hurt") }
    override val deathSound by lazy { Key.key("entity.axolotl.death") }

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
