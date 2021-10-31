package me.racci.sylphia.data

import me.racci.sylphia.Sylphia
import java.util.*
import java.util.concurrent.ConcurrentHashMap

internal object PlayerManager {

    private val playerData = ConcurrentHashMap<UUID, PlayerData>()

    operator fun get(uuid: UUID) = playerData[uuid]

    fun init() {

    }

    fun close() {
        playerData.keys.forEach(Sylphia.storageManager::save)
        playerData.clear()
        playerData.clear()
    }

    fun removePlayerData(uuid: UUID) {
        playerData.remove(uuid)
    }

    fun addPlayerData(playerData: PlayerData) {
        this.playerData[playerData.uuid] = playerData
    }

}