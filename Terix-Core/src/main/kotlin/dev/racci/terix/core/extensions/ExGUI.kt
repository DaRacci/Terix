package dev.racci.terix.core.extensions

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.pane.OutlinePane
import com.github.stefvanschie.inventoryframework.pane.util.Mask
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

fun ItemStack.asGuiItem(action: (InventoryClickEvent.() -> Unit)? = null): GuiItem = GuiItem(this, action)

class OutlinePane(
    x: Int,
    y: Int,
    length: Int,
    height: Int,
    priority: Priority,
    block: (dev.racci.terix.core.extensions.OutlinePane.() -> Unit)? = null
) : OutlinePane(x, y, length, height, priority) {

    fun setMask(mask: Mask) = applyMask(mask)

    init {
        block?.invoke(this)
    }
}
