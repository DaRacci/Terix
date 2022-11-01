package dev.racci.terix.api.origins

import dev.racci.minix.api.extensions.parse
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material

public class OriginItem {

    public var name: Component? = null
    public var material: Material = Material.AIR
    public var loreComponents: List<Component> = emptyList()

    /**
     * Sets the lore of this item, meant to be used with a multiline String surrounded by `"""`.
     * Each line will need styling as they are parsed individually as [Component]s.
     */
    public var lore: String
        get() = loreComponents.joinToString("\n") { MiniMessage.miniMessage().serialize(it) }
        set(value) { loreComponents = value.split('\n').map(String::parse) }

    override fun toString(): String = "OriginItem(name=$name, material=$material, loreComponents=$loreComponents)"
}
