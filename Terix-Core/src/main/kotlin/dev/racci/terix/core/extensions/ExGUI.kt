package dev.racci.terix.core.extensions

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.pane.OutlinePane
import com.github.stefvanschie.inventoryframework.pane.util.Mask
import dev.racci.minix.api.extensions.asInt
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

fun ItemStack.asGuiItem(action: (InventoryClickEvent.() -> Unit)? = null): GuiItem = GuiItem(this, action)

class OutlinePane(
    x: Int,
    y: Int,
    length: Int,
    height: Int,
    priority: Priority,
    block: (dev.racci.terix.core.extensions.OutlinePane.() -> Unit)? = null,
) : OutlinePane(x, y, length, height, priority) {

    fun setMask(mask: Mask) = applyMask(mask)

    init {
        block?.invoke(this)
    }
}

fun dev.racci.terix.core.extensions.OutlinePane.borderMask() {
    val stringList = mutableListOf<String>()
    for (row in 0 until height) {
        var rowString = ""
        for (col in 0 until length) {
            rowString += (
                row == 0 ||
                    row == height - 1 ||
                    col == 0 || col == length - 1
                ).asInt()
        }
        stringList += rowString
    }
    mask = Mask(*stringList.toTypedArray())
}
