@file:Suppress("unused")
@file:JvmName("StorageProvider")
package me.racci.sylphia.data.storage

import me.racci.sylphia.Sylphia
import me.racci.sylphia.data.PlayerData
import me.racci.sylphia.data.PlayerManager
import me.racci.sylphia.events.DataLoadEvent
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable

abstract class StorageProvider protected constructor(plugin: Sylphia) {
    @JvmField
    val plugin: Sylphia
    @JvmField
    val playerManager: PlayerManager?
    fun createNewPlayer(player: Player): PlayerData {
        val playerData = PlayerData(player, plugin)
        playerManager!!.addPlayerData(playerData)
        val event = DataLoadEvent(playerData)
        object : BukkitRunnable() {
            override fun run() {
                Bukkit.getPluginManager().callEvent(event)
            }
        }.runTask(plugin)
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

    init {
        playerManager = plugin.playerManager
        this.plugin = plugin
    }
}