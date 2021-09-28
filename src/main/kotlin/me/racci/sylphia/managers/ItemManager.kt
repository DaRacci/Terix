package me.racci.sylphia.managers

import me.racci.raccicore.utils.items.builders.ItemBuilder
import me.racci.raccicore.utils.strings.LegacyUtils
import me.racci.raccicore.utils.strings.colour
import me.racci.sylphia.data.configuration.Option
import me.racci.sylphia.data.configuration.OptionL
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

class ItemManager {

    val items = LinkedHashMap<Item, ItemStack>()

    init {
        loadItems()
    }

    fun loadItems() {
        items.clear()
        items[Item.ORIGIN_TOKEN] = ItemBuilder.from(ItemStack(OptionL.getMaterial(Option.ORIGIN_TOKEN_MATERIAL)))
            .unbreakable()
            .flags(ItemFlag.HIDE_UNBREAKABLE)
            .setNbt("OriginToken", true)
            .glow(OptionL.getBoolean(Option.ORIGIN_TOKEN_ENCHANTED))
            .name(LegacyUtils.parseLegacy(colour(OptionL.getString(Option.ORIGIN_TOKEN_NAME))).decoration(TextDecoration.ITALIC, false))
            .lore(LegacyUtils.parseLegacy(colour(OptionL.getList(Option.ORIGIN_TOKEN_LORE))))
            .build()!!
    }

}

enum class Item {
    ORIGIN_TOKEN
}