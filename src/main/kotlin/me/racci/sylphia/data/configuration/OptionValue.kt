@file:Suppress("unused")
@file:JvmName("OptionValue")
package me.racci.sylphia.data.configuration

class OptionValue(var value: Any?) {

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
}


