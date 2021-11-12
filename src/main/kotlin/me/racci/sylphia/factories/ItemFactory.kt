package me.racci.sylphia.factories

import me.racci.raccicore.interfaces.IFactory
import org.bukkit.inventory.ItemStack

object ItemFactory: IFactory<ItemFactory> {

    override fun init() {

    }

    override fun close() {

    }

    override fun reload() {

    }


}

data class GUIItem(val item: ItemStack, var slot: Int) {

}