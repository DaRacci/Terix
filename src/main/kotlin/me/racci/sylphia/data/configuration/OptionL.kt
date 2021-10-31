package me.racci.sylphia.data.configuration

import me.racci.raccicore.Level
import me.racci.raccicore.log
import me.racci.raccicore.utils.strings.colour
import me.racci.sylphia.Sylphia
import me.racci.sylphia.enums.ValueType.*
import org.apache.commons.lang3.EnumUtils
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.Sound
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
            val value = config[option.path]
            if(value != null) {
                when(option.type) {
                    INT, DOUBLE -> {
                        if(value is Int || value is Double) {
                            options[option] = OptionValue(value)
                            loaded++
                        } else optionError(option)
                    }
                    BOOLEAN -> {
                        if(value is Boolean) {
                            options[option] = OptionValue(value)
                            loaded++
                        } else optionError(option)
                    }
                    STRING -> {
                        if(value is String || value is Int || value is Double || value is Boolean) {
                            options[option] = OptionValue((colour(value.toString())) as Any)
                            loaded++
                        } else optionError(option)
                    }
                    LIST -> {
                        if(value is List<*>) {
                            options[option] = OptionValue((colour(value as List<String>) as Any))
                            loaded++
                        } else optionError(option)
                    }
                    COLOR -> {
                        if(EnumUtils.isValidEnumIgnoreCase(ChatColor::class.java, value as String?)) {
                            options[option] = OptionValue(value)
                            loaded++
                        } else optionError(option)
                    }
                    SOUND -> {
                        if(EnumUtils.isValidEnumIgnoreCase(Sound::class.java, value as String?)) {
                            options[option] = OptionValue(value)
                            loaded++
                        } else optionError(option)
                    }
                    MATERIAL -> {
                        if(EnumUtils.isValidEnumIgnoreCase(Material::class.java, value as String?)) {
                            options[option] = OptionValue(value)
                            loaded++
                        } else optionError(option)
                    }
                    MATERIAL_HEAD -> TODO()
                    EFFECT -> TODO()
                    ATTRIBUTE -> TODO()
                }
            } else {
                log(Level.WARNING,"Missing value in config.yml: Option " + option.name + " with path " + option.path + " was not found, using default value instead!")
            }
        }
        val end = System.currentTimeMillis()
        log(Level.INFO, "Loaded " + loaded + " config options in " + (end - start) + " ms")
    }

    private fun optionError(option: Option) {
        log(Level.WARNING,"Missing value in config.yml: Option " + option.name + " with path " + option.path + " was not found, using default value instead!")
    }

    private fun loadDefaultOptions() {
        val inputStream = plugin.getResource("config.yml")
        if (inputStream != null) {
            val config: FileConfiguration = YamlConfiguration.loadConfiguration(InputStreamReader(inputStream))
            for (option in Option.values()) {
                if (option.type === INT) {
                    options[option] = OptionValue(config.getInt(option.path))
                } else if (option.type === DOUBLE) {
                    options[option] = OptionValue(config.getDouble(option.path))
                } else if (option.type === BOOLEAN) {
                    options[option] = OptionValue(config.getBoolean(option.path))
                }
            }
        }
    }

    companion object {
        private val options: MutableMap<Option, OptionValue> = EnumMap(
            Option::class.java
        )

        fun getInt(option: Option): Int {
            return options[option]!!.asInt()
        }
        fun getDouble(option: Option): Double {
            return options[option]!!.asDouble()
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
        fun getColour(option: Option): ChatColor {
            return options[option]!!.asColour()
        }
        fun getSound(option: Option): Sound {
            return options[option]!!.asSound()
        }
        fun getMaterial(option: Option): Material {
            return options[option]!!.asMaterial()
        }
    }
}