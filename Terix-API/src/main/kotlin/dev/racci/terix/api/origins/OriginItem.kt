package dev.racci.terix.api.origins

import dev.racci.minix.api.extensions.parse
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material

class OriginItem {

    var name: Component? = null
    var material: Material = Material.AIR
    var loreComponents: List<Component> = emptyList()

    /**
     * Sets the lore of this item, meant to be used with a multiline String surrounded by `"""`.
     * Each line will need styling as they are parsed individually as [Component]s.
     */
    var lore: String
        get() = loreComponents.joinToString("\n") { MiniMessage.miniMessage().serialize(it) }
        set(value) { loreComponents = value.split('\n').map(String::parse) }

    override fun toString() =
        "OriginItem(name=$name, material=$material, loreComponents=$loreComponents)"
}
