package me.racci.sylphia.data

import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class PlayerManager {

    private val playerData: ConcurrentHashMap<UUID, PlayerData> = ConcurrentHashMap()
    fun getPlayerData(player: Player): PlayerData? {
        return playerData[player.uniqueId]
    }

    fun getPlayerData(id: UUID): PlayerData? {
        return playerData[id]
    }

    fun addPlayerData(playerData: PlayerData) {
        this.playerData[playerData.player.uniqueId] = playerData
    }

    fun removePlayerData(id: UUID) {
        playerData.remove(id)
    }

    fun hasPlayerData(player: Player): Boolean {
        return playerData.containsKey(player.uniqueId)
    }

    val playerDataMap: ConcurrentMap<UUID, PlayerData>
        get() = playerData

}