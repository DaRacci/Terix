package me.racci.sylphia.lang;

public enum CommandMessage implements MessageKey {

    RELOAD("Reload"),
    SAVE_SAVED("Saved"),
    TOGGLE_ENABLED("Toggle.Enabled"),
    TOGGLE_DISABLED("Toggle.Disabled");

    private final String path;

    CommandMessage(String path) {
        this.path = "Messages.Commands." + path;
    }

    public String getPath() {
        return path;
    }

}
