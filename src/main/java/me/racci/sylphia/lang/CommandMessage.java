package me.racci.sylphia.lang;

import java.util.Locale;

public enum CommandMessage implements MessageKey {

    NO_PROFILE("no_profile"),
    RELOAD,
//    SAVE_ALREADY_SAVING(Command.SAVE, "already_saving"),
    SAVE_SAVED,
    TOGGLE_ENABLED,
    TOGGLE_DISABLED;
//    TOGGLE_NOT_ENABLED(Command.TOGGLE, "not_enabled");

    private final String path;

    CommandMessage() {
        this.path = "commands." + this.name().toLowerCase(Locale.ENGLISH).replace("_", ".");
    }
    
//    CommandMessage(Command command, String path) {
//        this.path = "commands." + command.name().toLowerCase(Locale.ENGLISH).replace("_", ".") + "." + path;
//    }

    CommandMessage(String path) {
        this.path = "commands." + path;
    }

    public String getPath() {
        return path;
    }

}
