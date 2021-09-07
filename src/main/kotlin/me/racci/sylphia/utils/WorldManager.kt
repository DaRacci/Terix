@file:Suppress("unused")
@file:JvmName("WorldManager")
package me.racci.sylphia.utils

import me.racci.raccilib.Level
import me.racci.raccilib.log
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.plugin.Plugin

class WorldManager(private val plugin: Plugin) {
    private var blockedWorlds: ArrayList<String> = ArrayList()
    private var disabledWorlds: ArrayList<String> = ArrayList()
    private var blockedCheckBlockReplaceWorlds: ArrayList<String>? = null
    fun loadWorlds() {
        var blockedWorldsLoaded = 0
        blockedWorlds.clear()
        disabledWorlds.clear()
        val config = plugin.config
        for (blockedWorld in config.getStringList("blocked-worlds")) {
            blockedWorlds.add(blockedWorld)
            blockedWorldsLoaded++
        }
        for (blockedWorld in config.getStringList("disabled-worlds")) {
            disabledWorlds.add(blockedWorld)
            blockedWorldsLoaded++
        }
        log(Level.INFO, "Loaded $blockedWorldsLoaded blocked worlds.")
    }

    fun isInBlockedWorld(location: Location): Boolean {
        if (location.world == null) {
            return false
        }
        val world = location.world
        return disabledWorlds.contains(world.name) || blockedWorlds.contains(world.name)
    }

    fun isInDisabledWorld(location: Location): Boolean {
        if (location.world == null) {
            return false
        }
        val world = location.world
        return disabledWorlds.contains(world.name)
    }

    fun isInBlockedCheckWorld(location: Location): Boolean {
        if (location.world == null) {
            return false
        }
        val world = location.world
        return blockedCheckBlockReplaceWorlds!!.contains(world.name)
    }

    fun isDisabledWorld(world: World): Boolean {
        return disabledWorlds.contains(world.name)
    }
}