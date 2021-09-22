@file:Suppress("unused")
package me.racci.sylphia.extensions

import me.racci.raccilib.utils.worlds.WorldTime
import me.racci.sylphia.Sylphia
import me.racci.sylphia.origins.OriginManager
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect

object PotionEffectExtension {

    val PotionEffect.origin
        get() = !this.hasIcon() || this.isAmbient || this.duration >= 86400


}

object PlayerExtension {

    var Player.currentOrigin
        get() = Sylphia.instance.originManager.getOrigin(this.uniqueId)
        set(origin) {
            val playerData = Sylphia.instance.playerManager.getPlayerData(this.uniqueId)
            playerData?.lastOrigin = playerData?.origin ?: "LOST"
            playerData?.origin = origin?.name?.uppercase() ?: "LOST"
        }
    val Player.previousOrigin
        get() = OriginManager.valueOf(Sylphia.instance.playerManager.getPlayerData(this.uniqueId)?.lastOrigin.orEmpty())

    val Player.hasOrigin
        get() = OriginManager.contains(Sylphia.instance.playerManager.getPlayerData(this.uniqueId)?.origin.orEmpty())

    val Player.isDay
        get() = WorldTime.isDay(this)
    val Player.isNight
        get() = WorldTime.isNight(this)

}
