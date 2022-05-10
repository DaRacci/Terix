package dev.racci.terix.core.origins

import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.Trigger
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material

class DragonOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name = "Dragon"
    override val colour = NamedTextColor.LIGHT_PURPLE!!
    override val hurtSound = Key.key("entity.enderman.hurt")
    override val deathSound = Key.key("entity.enderman.death")

    override suspend fun onRegister() {
        damage {
            Trigger.WET += 2.0
        }
        item {
            material = Material.DRAGON_BREATH
            lore = "<light_purple>A breath of fire that can be used to summon a dragon."
        }
    }
}
