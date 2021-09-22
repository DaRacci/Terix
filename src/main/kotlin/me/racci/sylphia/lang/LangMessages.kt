@file:Suppress("unused")
@file:JvmName("LangMessages")
package me.racci.sylphia.lang

import java.util.*

interface MessageKey {

    val path: String

    companion object {
        fun values(): Set<MessageKey> {
            val keys: MutableSet<MessageKey> = HashSet()
            keys.addAll(listOf(*Empty.values()))
            keys.addAll(listOf(*Command.values()))
            keys.addAll(listOf(*GUI.values()))
            keys.addAll(listOf(*Prefix.values()))
            keys.addAll(listOf(*Origins.values()))
            return keys
        }
    }
}

enum class Empty : MessageKey {
    EMPTY;

    override val path: String = ""

}

enum class Prefix(path: String) : MessageKey {
    SYLPHIA("Sylphia"),
    ERROR("Error"),
    ORIGINS("Origins");

    override val path: String

    init {
        this.path = "Prefixes.$path"
    }
}

enum class GUI(path: String) : MessageKey {

    CLOSE("GUI.Common.Close"),
    BACK("GUI.Common.Back"),
    ACCEPT("GUI.Common.Accept"),
    DENY("GUI.Common.Deny"),

    SELECTION_TITLE("GUI.Selection.Title"),
    SELECTION_CUSTOM_TITLE("GUI.Selection.Custom_Title"),
    SELECTION_CONFIRM_TITLE("GUI.Selection.Confirm_Title"),

    INFO_MENU_TITLE("GUI.Info.Title"),
    POWERS("GUI.Info.Powers"),
    PASSIVES("GUI.Info.Passives"),
    DEBUFFS("GUI.Info.Debuffs");

    override val path: String

    init {
        this.path = path
    }

}

enum class Command(path: String) : MessageKey {
    RELOAD("Reload"),
    SAVE_SAVED("Saved"),
    TOGGLE_ENABLED("Toggle.Enabled"),
    TOGGLE_CURRENT("Toggle.Current"),
    TOGGLE_SPECIFIC("Toggle.Specific"),
    TOGGLE_DISABLED("Toggle.Disabled"),
    TOGGLE_NO_PERMISSION("Toggle.NoPermission");

    override val path: String

    init {
        this.path = "Messages.Commands.$path"
    }
}

enum class Origins(path: String) : MessageKey {

    COMMAND_GET_HAS("Origins.Command.Get.HasOrigin"),
    COMMAND_GET_NULL("Origins.Command.Get.NoOrigin"),
    COMMAND_SET_SUCCESS("Origins.Command.Set.Success"),
    COMMAND_SET_CURRENT("Origins.Command.Set.Current"),
    COMMAND_RESET_SENDER("Origins.Command.Reset.Sender"),
    COMMAND_RESET_TARGET("Origins.Command.Reset.Target"),
    COMMAND_TOKEN_SENDER("Origins.Command.Token.Sender"),
    COMMAND_TOKEN_TARGET("Origins.Command.Token.Target"),

    SELECT_BROADCAST("Origins.Select.Broadcast"),
    SELECT_LOCKED("Origins.Select.Locked"),
    SELECT_CURRENT("Origins.Select.Current"),

    LORE_CENTERED("Origins.Lore.Centered"),
    LORE_INDENT("Origins.Lore.Indent"),
    LORE_PASSIVES("Origins.Lore.Passives"),
    LORE_ABILITIES("Origins.Lore.Abilities"),
    LORE_DEBUFFS("Origins.Lore.Debuffs"),
    LORE_SELECT("Origins.Lore.Select");

    override val path: String

    init {
        this.path = path
    }
}

class CustomMessageKey(path: String) : MessageKey {

    override val path: String

    init {
        this.path = path
    }
    override fun hashCode(): Int {
        return Objects.hashCode(path)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CustomMessageKey

        if (path != other.path) return false

        return true
    }
}