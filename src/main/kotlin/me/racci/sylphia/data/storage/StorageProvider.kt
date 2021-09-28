package me.racci.sylphia.data.storage

import me.racci.sylphia.Sylphia
import me.racci.sylphia.data.PlayerData
import me.racci.sylphia.data.PlayerManager
import me.racci.sylphia.events.DataLoadEvent
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player

abstract class StorageProvider protected constructor(internal val plugin: Sylphia) {

    val playerManager: PlayerManager = plugin.playerManager
    fun createNewPlayer(player: Player): PlayerData {
        val playerData = PlayerData(player, plugin)
        playerManager.addPlayerData(playerData)
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

    abstract fun load(player: Player)
    abstract fun save(player: Player)
    abstract fun save(player: Player, removeFromMemory: Boolean)
}