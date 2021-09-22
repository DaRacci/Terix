@file:Suppress("unused")
@file:JvmName("Option")
package me.racci.sylphia.data.configuration

import me.racci.sylphia.enums.ValueType

enum class Option(val path: String,
                  val type: ValueType,
                  val parseColour: Boolean = false) {

    ORIGIN_TOKEN_MATERIAL("Origin-Token.Material", ValueType.MATERIAL),
    ORIGIN_TOKEN_ENCHANTED("Origin-Token.Enchanted", ValueType.BOOLEAN),
    ORIGIN_TOKEN_AMOUNT("Origin-Token.Amount", ValueType.INT),
    ORIGIN_TOKEN_NAME("Origin-Token.Name", ValueType.STRING, true),
    ORIGIN_TOKEN_LORE("Origin-Token.Lore", ValueType.LIST, true),
    SOUND_VOLUME("Sound-Volume", ValueType.DOUBLE),
    BLACKLISTED_WORLDS("Blacklisted-Worlds", ValueType.LIST),
    PREVIEW_WORLDS("Preview-Worlds", ValueType.LIST);
}