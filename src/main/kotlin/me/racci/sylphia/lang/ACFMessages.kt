@file:Suppress("unused")
@file:JvmName("ACFMessages")
package me.racci.sylphia.lang

enum class ACFCoreMessage {
    PERMISSION_DENIED,
    PERMISSION_DENIED_PARAMETER,
    ERROR_GENERIC_LOGGED,
    UNKNOWN_COMMAND,
    INVALID_SYNTAX,
    ERROR_PREFIX,
    ERROR_PERFORMING_COMMAND,
    INFO_MESSAGE, PLEASE_SPECIFY_ONE_OF,
    MUST_BE_A_NUMBER, MUST_BE_MIN_LENGTH,
    MUST_BE_MAX_LENGTH,
    PLEASE_SPECIFY_AT_LEAST,
    PLEASE_SPECIFY_AT_MOST,
    NOT_ALLOWED_ON_CONSOLE,
    COULD_NOT_FIND_PLAYER,
    NO_COMMAND_MATCHED_SEARCH,
    HELP_PAGE_INFORMATION,
    HELP_NO_RESULTS,
    HELP_HEADER,
    HELP_FORMAT,
    HELP_DETAILED_HEADER,
    HELP_DETAILED_COMMAND_FORMAT,
    HELP_DETAILED_PARAMETER_FORMAT,
    HELP_SEARCH_HEADER;

    val path: String
        get() = Companion.path

    companion object {
        private const val path = "Messages.Core"
    }
}

enum class ACFMinecraftMessage {
    INVALID_WORLD,
    YOU_MUST_BE_HOLDING_ITEM,
    PLAYER_IS_VANISHED_CONFIRM,
    USERNAME_TOO_SHORT,
    IS_NOT_A_VALID_NAME,
    MULTIPLE_PLAYERS_MATCH,
    NO_PLAYER_FOUND_SERVER,
    NO_PLAYER_FOUND_OFFLINE,
    NO_PLAYER_FOUND,
    LOCATION_PLEASE_SPECIFY_WORLD,
    LOCATION_PLEASE_SPECIFY_XYZ,
    LOCATION_CONSOLE_NOT_RELATIVE;

    val path: String
        get() = Companion.path

    companion object {
        private const val path = "Messages.Minecraft"
    }
}