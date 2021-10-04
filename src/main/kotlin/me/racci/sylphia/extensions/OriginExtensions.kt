package me.racci.sylphia.extensions

import me.racci.raccicore.utils.worlds.WorldTime
import me.racci.sylphia.originManager
import me.racci.sylphia.origins.OriginManager
import me.racci.sylphia.playerManager
import org.bukkit.entity.Player

object PlayerExtension {

    var Player.currentOrigin
        get() = originManager.getOrigin(this.uniqueId)
        set(origin) {
            val playerData = playerManager.getPlayerData(this.uniqueId)
            playerData?.lastOrigin = playerData?.origin ?: "LOST"
            playerData?.origin = origin?.identity?.name?.uppercase() ?: "LOST"
        }
    val Player.previousOrigin
        get() = OriginManager.valueOf(playerManager.getPlayerData(this.uniqueId)?.lastOrigin.orEmpty())

    val Player.hasOrigin
        get() = OriginManager.contains(playerManager.getPlayerData(this.uniqueId)?.origin.orEmpty())

    val Player.isDay
        get() = WorldTime.isDay(this)
    val Player.isNight
        get() = WorldTime.isNight(this)

}
