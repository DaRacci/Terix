package me.racci.sylphia.data.storage

import me.racci.sylphia.Sylphia
import me.racci.sylphia.data.PlayerData
import me.racci.sylphia.enums.Special
import me.racci.sylphia.events.DataLoadEvent
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.util.*

class YamlStorageProvider(plugin: Sylphia) : StorageProvider(plugin) {
    override fun load(player: Player) {
        val file = File(plugin.dataFolder.toString() + "/Players/" + player.uniqueId + ".yml")
        if (file.exists()) {
            val config: FileConfiguration = YamlConfiguration.loadConfiguration(file)
            val playerData = PlayerData(player, plugin)
            try {
                // Make sure file name and uuid match
                val id: UUID = UUID.fromString(config.getString("uuid", player.uniqueId.toString()))
                require(player.uniqueId == id) { "File name and uuid field do not match!" }
                // Load origin data
                playerData.origin = config.getString("Origins.Origins")
                playerData.lastOrigin = config.getString("Origins.LastOrigin")
                for (originSetting in Special.values()) {
                    val path = "Settings." + originSetting.name.uppercase()
                    val value = config.getInt(path, 1)
                    playerData.setOriginSetting(originSetting, value)
                }
                playerManager!!.addPlayerData(playerData)
                val event = DataLoadEvent(playerData)
                object : BukkitRunnable() {
                    override fun run() {
                        Bukkit.getPluginManager().callEvent(event)
                    }
                }.runTask(plugin)
            } catch (e: Exception) {
                Bukkit.getLogger()
                    .warning("There was an error loading player data for player " + player.name + " with UUID " + player.uniqueId + ", see below for details.")
                e.printStackTrace()
                val data = createNewPlayer(player)
                data.setShouldSave(false)
                sendErrorMessageToPlayer(player, e)
            }
        } else {
            createNewPlayer(player)
        }
    }

    override fun save(player: Player, removeFromMemory: Boolean) {
        val playerData = playerManager!!.getPlayerData(player) ?: return
        if (playerData.shouldNotSave()) return
        // Save lock
        if (playerData.isSaving) return
        playerData.isSaving = true
        // Load file
        val file = File(plugin.dataFolder.toString() + "/Players/" + player.uniqueId + ".yml")
        val config: FileConfiguration = YamlConfiguration.loadConfiguration(file)
        try {
            config["User-Info.Username"] = player.name
            config["User-Info.UUID"] = player.uniqueId.toString()
            // Save origin data
            if (playerData.origin != null) {
                config["Origins.Origins"] = playerData.origin
            } else {
                config["Origins.Origins"] = ""
            }
            if (playerData.lastOrigin != null) {
                config["Origins.LastOrigin"] = playerData.lastOrigin
            } else {
                config["Origins.LastOrigin"] = ""
            }
            for (originSetting in Special.values()) {
                val path = "Settings.$originSetting"
                config[path] = playerData.getOriginSetting(originSetting)
            }
            config.save(file)
            if (removeFromMemory) {
                playerManager.removePlayerData(player.uniqueId) // Remove from memory
            }
        } catch (e: Exception) {
            Bukkit.getLogger()
                .warning("There was an error saving player data for player " + player.name + " with UUID " + player.uniqueId + ", see below for details.")
            e.printStackTrace()
        }
        playerData.isSaving = false // Unlock
    }

    override fun save(player: Player) {
        save(player, false)
    }
}