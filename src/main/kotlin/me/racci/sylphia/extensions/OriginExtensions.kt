package me.racci.sylphia.extensions

import me.racci.raccicore.utils.worlds.WorldTime
import me.racci.sylphia.Sylphia
import me.racci.sylphia.data.PlayerManager
import me.racci.sylphia.origins.OriginManager
import org.bukkit.entity.Player

object PlayerExtension {

    var Player.currentOrigin
        get() = OriginManager.getOrigin(uniqueId)
        set(origin) {
            require(origin != null) {"Origin must not be null."}
            val playerData = PlayerManager[uniqueId]
            playerData.lastOrigin = playerData.origin ?: "LOST"
            playerData.origin = origin.identity.name.uppercase()
            Sylphia.storageManager.save(uniqueId)
        }

    val Player.previousOrigin
        get() = OriginManager.valueOf(PlayerManager[uniqueId].lastOrigin.orEmpty())

    val Player.hasOrigin
        get() = OriginManager.contains(PlayerManager[uniqueId].origin.orEmpty())

    val Player.isDay
        get() = WorldTime.isDay(this)

    val Player.isNight
        get() = WorldTime.isNight(this)

}
