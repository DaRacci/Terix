package dev.racci.terix.core.origins

import dev.racci.minix.api.builders.ItemBuilderDSL
import dev.racci.minix.api.plugin.MinixPlugin
import dev.racci.terix.api.origins.AbstractOrigin
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.event.entity.EntityDamageEvent

class AngelOrigin(plugin: MinixPlugin) : AbstractOrigin(plugin) {

    override val name = "Angel"
    override val colour by lazy { TextColor.fromHexString("#fff6cc")!! }
    override val hurtSound by lazy { Key.key("entity.bat.hurt") }
    override val deathSound by lazy { Key.key("entity.bat.death") }
    override val guiItem by lazy {
        ItemBuilderDSL.from(Material.FEATHER) {
            name = displayName
        }
    }

    override suspend fun onRegister() {
        attributes {
            Attribute.GENERIC_MAX_HEALTH setBase 16.0
        }
        damage {
            EntityDamageEvent.DamageCause.FALL multiplied 0.0
            listOf(
                EntityDamageEvent.DamageCause.LAVA,
                EntityDamageEvent.DamageCause.FIRE,
                EntityDamageEvent.DamageCause.FIRE_TICK,
                EntityDamageEvent.DamageCause.HOT_FLOOR,
                EntityDamageEvent.DamageCause.LIGHTNING,
            ) multiplied 2.0
        }
    }
}
