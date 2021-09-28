package me.racci.sylphia.data.configuration

import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.Sound

class OptionValue(var value: Any) {

    fun setValue(value: Int) {
        this.value = value
    }
    fun setValue(value: Double) {
        this.value = value
    }
    fun setValue(value: Boolean) {
        this.value = value
    }
    fun setValue(value: String) {
        this.value = value
    }
    fun setValue(value: List<String>) {
        this.value = value
    }
    fun setValue(value: ChatColor) {
        this.value = value
    }
    fun setValue(value: Sound) {
        this.value = value
    }
    fun setValue(value: Material) {
        this.value =value
    }

    fun asInt(): Int {
        return value as Int
    }
    fun asDouble(): Double {
        return if(value !is Int) {
            value as Double
        } else (value as Int).toDouble()

    }
    fun asBoolean(): Boolean {
        return value as Boolean
    }
    fun asString(): String {
        return if(value is String) {
            value as String
        } else value.toString()
    }
    fun asList(): List<String> {
        val stringList = ArrayList<String>()
        if(value is List<*>) {
            for(any: Any? in value as List<*>) {
                if(any is String) {
                    stringList.add(any)
                }
            }
        }
        return stringList
    }
    fun asColour(): ChatColor {
        return if(value is ChatColor) {
            value as ChatColor
        } else ChatColor.valueOf(value.toString())
    }
    fun asSound(): Sound {
        return if(value is Sound) {
            value as Sound
        } else Sound.valueOf(value.toString())
    }
    fun asMaterial(): Material {
        return if(value is Material) {
            value as Material
        } else Material.valueOf(value.toString())
    }
}


