package dev.racci.terix.api.data

import org.bukkit.Material
import org.bukkit.inventory.ItemStack

public fun interface ItemMatcher {
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("matchesReceiver")
    public fun ItemStack.matches(): Boolean

    public fun matches(stack: ItemStack): Boolean = stack.matches()

    public companion object {
        public fun of(material: Material): ItemMatcher = ItemMatcher { this.type === material }
    }
}
