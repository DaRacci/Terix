package me.racci.sylphia.gui;

public enum ItemType {

    ACCEPT("Items.Accept."),
    DENY("Items.Deny."),
    BACK("Items.Back."),
    EXIT("Items.Exit."),
    NEXT_PAGE("Items.Page.NextPage."),
    PREVIOUS_PAGE("Items.Page.PreviousPage."),
    OUTLINE("Items.Filler.Outline"),
    BACKGROUND("Items.Filler.Background");

    private final String path;

    ItemType(String path) {
        this.path = path;
    }

    public String getMaterial() {
        return path + "Material";
    }
    public String getName() {
        return path + "Name";
    }
    public String getLore() {
        return path + "Lore";
    }


}
