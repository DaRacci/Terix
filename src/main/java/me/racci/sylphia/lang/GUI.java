package me.racci.sylphia.lang;

public enum GUI implements MessageKey {

    // Common
    CLOSE("GUI.Common.Close"),
    ACCEPT("GUI.Common.Accept"),
    DENY("GUI.Common.Deny"),
    // Selection menu
    SELECTION_TITLE("GUI.Selection.Title"),
    SELECTION_CUSTOM_TITLE("GUI.Selection.Custom_Title"),
    SELECTION_CONFIRM_TITLE("GUI.Selection.Confirm_Title"),
    // Info menu
    INFO_MENU_TITLE("GUI.Info.Title"),
    POWERS("GUI.Info.Powers"),
    PASSIVES("GUI.Info.Passives"),
    DEBUFFS("GUI.Info.Debuffs");
    
    private final String path;

    GUI(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
