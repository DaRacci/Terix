@file:Suppress("unused")
@file:JvmName("Option")
package me.racci.sylphia.data.configuration

enum class Option(val path: String,
                  val type: OptionType) {
    SOUND_VOLUME("Sound-Volume", OptionType.DOUBLE),
    BLACKLISTED_WORLDS("Blacklisted-Worlds", OptionType.LIST),
    PREVIEW_WORLDS("Preview-Worlds", OptionType.LIST);
}