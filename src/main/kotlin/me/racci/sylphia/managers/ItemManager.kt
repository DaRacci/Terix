package me.racci.sylphia.managers

import me.racci.raccicore.builders.ItemBuilder
import me.racci.raccicore.utils.strings.LegacyUtils
import me.racci.raccicore.utils.strings.colour
import me.racci.sylphia.data.configuration.Option
import me.racci.sylphia.data.configuration.OptionL
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

internal object ItemManager {

    operator fun get(item: Item) = items[item]

    private val items = LinkedHashMap<Item, ItemStack>()

    fun init() {
        loadItems()
    }

    fun close() {
        items.clear()
    }

    fun loadItems() {
        items[Item.ORIGIN_TOKEN] = ItemBuilder.from(ItemStack(OptionL.getMaterial(Option.ORIGIN_TOKEN_MATERIAL))) {
            unbreakable = true
            addFlag(ItemFlag.HIDE_UNBREAKABLE)
            nbt = "OriginToken" to true
            glow = OptionL.getBoolean(Option.ORIGIN_TOKEN_ENCHANTED)
            name = LegacyUtils.parseLegacy(colour(OptionL.getString(Option.ORIGIN_TOKEN_NAME))).decoration(TextDecoration.ITALIC, false)
            lore{LegacyUtils.parseLegacy(colour(OptionL.getList(Option.ORIGIN_TOKEN_LORE)))}
        }
    }

}

enum class Item {
    ORIGIN_TOKEN
}