package me.racci.sylphia.lang;

import org.jetbrains.annotations.NotNull;

public enum CommandMessage implements MessageKey {

    RELOAD("Reload"),
    SAVE_SAVED("Saved"),
    TOGGLE_ENABLED("Toggle.Enabled"),
    TOGGLE_DISABLED("Toggle.Disabled");

    private final String path;

    CommandMessage(String path) {
        this.path = "Messages.Commands." + path;
    }

    @NotNull
    public String getPath() {
        return path;
    }

}
