package me.racci.sylphia.managers

import me.racci.sylphia.Sylphia
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Player

internal object WorldManager {

    private var disabledWorlds = ArrayList<String>()
    private var previewWorlds = ArrayList<String>()

    fun init(plugin: Sylphia) {
        plugin.config.getStringList("Blacklisted-Worlds").toCollection(disabledWorlds)
        plugin.config.getStringList("Preview-Worlds").toCollection(previewWorlds)
        val disabledWorldsLoaded = disabledWorlds.size
        val previewWorldsLoaded = previewWorlds.size
        plugin.log.info("Loaded $disabledWorldsLoaded blocked worlds, and $previewWorldsLoaded preview worlds.")
    }

    fun close() {
        disabledWorlds.clear()
        previewWorlds.clear()
    }

    fun isDisabledWorld(player: Player) =
        disabledWorlds.contains(player.world.name)

    fun isDisabledWorld(location: Location) =
        disabledWorlds.contains(location.world.name)

    fun isDisabledWorld(world: World) =
        disabledWorlds.contains(world.name)

    fun isPreviewWorld(player: Player) =
        previewWorlds.contains(player.world.name)

    fun isPreviewWorld(location: Location) =
        previewWorlds.contains(location.world.name)

    fun isPreviewWorld(world: World) =
        previewWorlds.contains(world.name)

}