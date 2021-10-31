package me.racci.sylphia.data

import me.racci.sylphia.enums.Special
import java.util.*

data class PlayerData(val uuid: UUID) {

    var origin      : String?   = null
    var lastOrigin  : String?   = null
    var isSaving    : Boolean   = false
    var shouldSave  : Boolean   = true
    private val originSettings  = EnumMap<Special, Int>(Special::class.java)
//    private val cooldownMap     = HashMap<String, Long>()

    operator fun get(special: Special): Int =
        originSettings.getOrDefault(special, 1)
    operator fun set(special: Special, value: Int) {
        originSettings[special] = value}

}

/*
    fun createCooldown(cooldown: String, length: Int) {
        cooldownMap["$cooldown.start"] = System.currentTimeMillis() / 50
        cooldownMap["$cooldown.time"] = length.toLong()
    }

    fun removeCooldown(cooldown: String) {
        cooldownMap.remove("cooldown.$cooldown")
    }

    private fun getStart(cooldown: String): Long {
        return cooldownMap.getOrDefault("$cooldown.start", (-1).toLong())
    }

    fun getCooldown(cooldown: String): Double {
        return cooldownMap.getOrDefault("$cooldown.time", (-1).toLong()).toDouble()
    }

    fun getTimeToExpire(cooldown: String): Long {
        return (if (getStart(cooldown) != -1L) getStart(cooldown) - System.currentTimeMillis() / 50.toDouble() + getCooldown(
            cooldown
        ) else -1).toLong()
    }
    */