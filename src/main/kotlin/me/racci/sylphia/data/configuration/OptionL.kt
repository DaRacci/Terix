@file:Suppress("unused")
@file:JvmName("OptionL")
package me.racci.sylphia.data.configuration

import me.racci.raccilib.Level
import me.racci.raccilib.log
import me.racci.sylphia.Sylphia
import org.bukkit.ChatColor
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.InputStreamReader
import java.util.*

class OptionL(private val plugin: Sylphia) {
    fun loadOptions() {
        loadDefaultOptions()
        val config = plugin.config
        var loaded = 0
        val start = System.currentTimeMillis()
        for (option in Option.values()) {
            //Get the value from config
            val value = config[option.path]
            //Check if value exists
            if (value != null) {
                //Add if supposed to be int and value is int
                if ((value is Int || value is Double) && option.type === OptionType.INT) {
                    options[option] = OptionValue(value)
                    loaded++
                } else if ((value is Double || value is Int) && option.type === OptionType.DOUBLE) {
                    options[option] = OptionValue(value)
                    loaded++
                } else if (value is Boolean && option.type === OptionType.BOOLEAN) {
                    options[option] = OptionValue(value)
                    loaded++
                } else if ((value is String || value is Int || value is Double || value is Boolean) && option.type === OptionType.STRING) {
                    options[option] = OptionValue(value.toString())
                    loaded++
                } else if (value is List<*> && option.type === OptionType.LIST) {
                    options[option] = OptionValue(value)
                    loaded++
                } else if (value is String && option.type === OptionType.COLOR) {
                    options[option] = OptionValue(ChatColor.valueOf(value.toString()))
                    loaded++
                } else {
                    log(
                        Level.WARNING,
                        "Incorrect type in config.yml: Option " + option.name + " with path " + option.path + " should be of type " + option.type.name + ", using default value instead!"
                    )
                }
            } else {
                log(
                    Level.WARNING,
                    "Missing value in config.yml: Option " + option.name + " with path " + option.path + " was not found, using default value instead!"
                )
            }
        }
        val end = System.currentTimeMillis()
        log(Level.WARNING, "Loaded " + loaded + " config options in " + (end - start) + " ms")
    }

    private fun loadDefaultOptions() {
        val inputStream = plugin.getResource("config.yml")
        if (inputStream != null) {
            val config: FileConfiguration = YamlConfiguration.loadConfiguration(InputStreamReader(inputStream))
            for (option in Option.values()) {
                if (option.type === OptionType.INT) {
                    options[option] = OptionValue(config.getInt(option.path))
                } else if (option.type === OptionType.DOUBLE) {
                    options[option] = OptionValue(config.getDouble(option.path))
                } else if (option.type === OptionType.BOOLEAN) {
                    options[option] = OptionValue(config.getBoolean(option.path))
                }
            }
        }
    }

    companion object {
        private val options: MutableMap<Option, OptionValue> = EnumMap(
            Option::class.java
        )

        fun getDouble(option: Option): Double {
            return options[option]!!.asDouble()
        }

        fun getInt(option: Option): Int {
            return options[option]!!.asInt()
        }

        fun getBoolean(option: Option): Boolean {
            return options[option]!!.asBoolean()
        }

        fun getString(option: Option): String {
            return options[option]!!.asString()
        }

        fun getList(option: Option): List<String> {
            return options[option]!!.asList()
        }
    }
}