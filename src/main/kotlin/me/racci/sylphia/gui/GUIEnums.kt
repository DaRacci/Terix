@file:Suppress("unused")
@file:JvmName("GUIEnums")
package me.racci.sylphia.gui

enum class MenuType(path: String) {

    SELECTOR("GUI.Selector."),
    CUSTOM("GUI.Custom."),
    CONFIRM("GUI.Confirm."),
    INFO("GUI.Info.");

    val path: String

    init {
        this.path = "Prefixes.$path"
    }
}

enum class ItemType(path: String) {
    ACCEPT("Items.Accept."),
    DENY("Items.Deny."),
    BACK("Items.Back."),
    EXIT("Items.Exit."),
    NEXT_PAGE("Items.Page.NextPage."),
    PREVIOUS_PAGE("Items.Page.PreviousPage."),
    OUTLINE("Items.Filler.Outline"),
    BACKGROUND("Items.Filler.Background");

    val path: String

    init {
        this.path = path
    }

    val itemMaterial: String
        get() = path + "Material"

    val itemName: String
        get() = path + "Name"

    val itemLore: String
        get() = path + "Lore"
}

enum class Type {
    CHEST,
    ENDER_CHEST,
    FURNACE,
    CRAFTING_TABLE,
    BREWING_STAND,
    HOPPER;
}