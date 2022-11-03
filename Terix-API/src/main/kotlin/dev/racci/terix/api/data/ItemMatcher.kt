package dev.racci.terix.api.data

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

public fun interface ItemMatcher {
    public fun ItemStack.actualMatcher(): Boolean

    public fun matches(stack: ItemStack): Boolean = stack.actualMatcher()

    public companion object {
        public fun of(material: Material): ItemMatcher = ItemMatcher { this.type === material }
    }
}
