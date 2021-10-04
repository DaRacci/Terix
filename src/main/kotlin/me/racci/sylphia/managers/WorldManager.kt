package me.racci.sylphia.managers

import me.racci.raccicore.Level
import me.racci.raccicore.log
import me.racci.sylphia.Sylphia
import me.racci.sylphia.plugin
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player

class WorldManager {

    private var disabledWorlds = ArrayList<String>()
    private var previewWorlds = ArrayList<String>()

    init {
        loadWorlds(plugin)
    }

    fun loadWorlds(plugin: Sylphia) {
        val config = plugin.config
        disabledWorlds.clear()
        previewWorlds.clear()
        config.getStringList("Blacklisted-Worlds").toCollection(disabledWorlds)
        config.getStringList("Preview-Worlds").toCollection(previewWorlds)
        val disabledWorldsLoaded = disabledWorlds.size
        val previewWorldsLoaded = previewWorlds.size
        log(Level.INFO, "Loaded $disabledWorldsLoaded blocked worlds, and $previewWorldsLoaded preview worlds.")
    }

    fun isDisabledWorld(player: Player) : Boolean {
        return disabledWorlds.contains(player.world.name)
    }
    fun isDisabledWorld(location: Location) : Boolean {
        return disabledWorlds.contains(location.world.name)
    }
    fun isDisabledWorld(world: World) : Boolean {
        return disabledWorlds.contains(world.name)
    }
    fun isPreviewWorld(player: Player) : Boolean {
        return previewWorlds.contains(player.world.name)
    }
    fun isPreviewWorld(location: Location) : Boolean {
        return previewWorlds.contains(location.world.name)
    }
    fun isPreviewWorld(world: World) : Boolean {
        return previewWorlds.contains(world.name)
    }
}