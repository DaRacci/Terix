package me.racci.sylphia.data

import me.racci.sylphia.enums.Special
import org.bukkit.entity.Player
import java.util.*

class PlayerData(val player: Player) {
    var origin: String? = null
    var lastOrigin: String? = null
    private val originSettings = EnumMap<Special, Int>(Special::class.java)
    private val cooldownMap = HashMap<String, Long>()
    var isSaving: Boolean = false
    private var shouldSave: Boolean

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

    fun getOriginSetting(originSetting: Special): Int {
        return originSettings.getOrDefault(originSetting, 1)
    }

    fun setOriginSetting(originSetting: Special, value: Int) {
        originSettings[originSetting] = value
    }

    fun shouldNotSave(): Boolean {
        return !shouldSave
    }

    fun setShouldSave(shouldSave: Boolean) {
        this.shouldSave = shouldSave
    }

    init {
        shouldSave = true
    }
}