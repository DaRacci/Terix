package dev.racci.terix.core.extension

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.util.Gui
import com.github.stefvanschie.inventoryframework.pane.OutlinePane
import com.github.stefvanschie.inventoryframework.pane.Pane
import com.github.stefvanschie.inventoryframework.pane.util.Mask
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

fun <T : Gui> T.dsl(block: T.() -> Unit): T {
    block(this)
    return this
}

fun <T : Pane> T.dsl(block: T.() -> Unit): T {
    block(this)
    return this
}

fun <T : Mask> T.dsl(block: T.() -> Unit): T {
    block(this)
    return this
}

var OutlinePane.repeat: Boolean get() = doesRepeat(); set(bool) { setRepeat(bool) }

fun ItemStack.asGuiItem(action: (InventoryClickEvent.() -> Unit)? = null): GuiItem = GuiItem(this, action)

class OutlinePane(
    x: Int,
    y: Int,
    length: Int,
    height: Int,
    priority: Priority,
    block: (dev.racci.terix.core.extension.OutlinePane.() -> Unit)? = null,
) : OutlinePane(x, y, length, height, priority) {

    fun setMask(mask: Mask) = applyMask(mask)

    init {
        block?.invoke(this)
    }
}

fun dev.racci.terix.core.extension.OutlinePane.borderMask() {
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
