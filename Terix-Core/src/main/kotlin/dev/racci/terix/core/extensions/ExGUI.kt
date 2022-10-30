package dev.racci.terix.core.extensions // ktlint-disable filename

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

fun ItemStack.asGuiItem(action: (InventoryClickEvent.() -> Unit)? = null): GuiItem = GuiItem(this, action)
