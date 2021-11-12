package me.racci.sylphia.data.storage

import me.racci.sylphia.data.PlayerData
import me.racci.sylphia.data.PlayerManager
import me.racci.sylphia.events.DataLoadEvent
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import java.util.UUID

abstract class StorageProvider {

    fun createNewPlayer(uuid: UUID): PlayerData {
        val playerData = PlayerData(uuid)
        PlayerManager.addPlayerData(playerData)
        val event = DataLoadEvent(playerData)
        Bukkit.getPluginManager().callEvent(event)
        return playerData
    }

    protected fun sendErrorMessageToPlayer(player: Player, e: Exception) {
        player.sendMessage(
            ChatColor.RED.toString() + "There was an error loading your origin data: " + e.message +
                    ". Please report the error to your server administrator. To prevent your data from resetting permanently" +
                    ", your origin data will not be saved. Try relogging to attempt loading again."
        )
    }

    abstract fun load(uuid: UUID)
    abstract fun save(uuid: UUID, removeFromMemory: Boolean = false)
}