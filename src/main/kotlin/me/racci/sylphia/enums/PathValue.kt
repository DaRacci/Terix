@file:Suppress("unused")
@file:JvmName("PathValue")
package me.racci.sylphia.enums

import org.bukkit.Sound

class PathValue(var value: Any) {
    fun setValue(value: Int) {
        this.value = value
    }

    fun setValue(value: Double) {
        this.value = value
    }

    fun setValue(value: Boolean) {
        this.value = value
    }

    fun asInt(): Int {
        return value as Int
    }

    fun asDouble(): Double {
        return if(value is Double) {
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
            for(obj: Any? in value as List<*>) {
                if(obj is String) {
                    stringList.add(value as String)
                }
            }
        }
        return stringList
    }

    fun asSound(): Sound? {
        return if(value is Sound) {
            value as Sound
        } else null
    }
}