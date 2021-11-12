package me.racci.sylphia.data.storage

import me.racci.raccicore.utils.catch
import me.racci.raccicore.utils.pm
import me.racci.sylphia.Sylphia
import me.racci.sylphia.data.PlayerData
import me.racci.sylphia.data.PlayerManager
import me.racci.sylphia.enums.Special
import me.racci.sylphia.events.DataLoadEvent
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.UUID

class YamlStorageProvider(val plugin: Sylphia) : StorageProvider() {

    override fun load(uuid: UUID) {
        val file = File("${plugin.dataFolder}/Players/$uuid.yml")
        if(file.exists()) {
            val config = YamlConfiguration.loadConfiguration(file)
            val playerData = PlayerData(uuid)
            catch<Exception>({
                plugin.log.error("There was an error loading player data for the player with the UUID $uuid, see below for details.")
                it.printStackTrace()
                val data = createNewPlayer(uuid)
                data.shouldSave = false
            }) {
                require(uuid == UUID.fromString(config.getString("uuid"))) {"File name and uuid field do not match!"}
                playerData.origin       = config.getString("Origins.Origin")
                playerData.lastOrigin   = config.getString("Origins.LastOrigin")
                Special.values().forEach{playerData[it] = config.getInt("Settings.${it.name.uppercase()}")}
                PlayerManager.addPlayerData(playerData)
                pm.callEvent(DataLoadEvent(playerData))
            }
        } else createNewPlayer(uuid)
    }

    override fun save(uuid: UUID, removeFromMemory: Boolean) {
        val playerData = PlayerManager[uuid]
        if(!playerData.shouldSave
            || playerData.isSaving
        ) return
        playerData.isSaving = true
        val file    = File("${plugin.dataFolder}/Players/$uuid.yml")
        val config  = YamlConfiguration.loadConfiguration(file)
        catch<Exception>({
            Sylphia.log.error("There was an error saving player data for the player with the UUID $uuid, see below for details.")
            it.printStackTrace()
        }) {
            config["UUID"]              = uuid
            config["Origins.Origin"]    = playerData.origin ?: ""
            config["Origins.LastOrigin"]= playerData.lastOrigin ?: ""
            Special.values().forEach{config["Settings.$it"] =
                playerData[it]}
            config.save(file)
            if(removeFromMemory) PlayerManager.removePlayerData(uuid)
        }
        playerData.isSaving = false
    }

}