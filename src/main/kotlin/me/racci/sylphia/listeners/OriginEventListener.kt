package me.racci.sylphia.listeners

import me.racci.raccicore.utils.extensions.KotlinListener
import me.racci.raccicore.utils.strings.LegacyUtils
import me.racci.raccicore.utils.strings.replace
import me.racci.sylphia.data.PlayerManager
import me.racci.sylphia.events.OriginChangeEvent
import me.racci.sylphia.events.OriginResetEvent
import me.racci.sylphia.extensions.PlayerExtension.currentOrigin
import me.racci.sylphia.lang.Lang
import me.racci.sylphia.lang.Origins
import me.racci.sylphia.origins.OriginManager
import me.racci.sylphia.runnables.RainRunnable
import me.racci.sylphia.runnables.SunLightRunnable
import me.racci.sylphia.runnables.WaterRunnable
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

class OriginEventListener : KotlinListener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onOriginChange(event: OriginChangeEvent) {

        val player = event.player
        val origin = event.newOrigin

        if(origin == event.oldOrigin) {
            event.sender?.sendMessage(Lang[Origins.COMMAND_SET_CURRENT])
            event.sender?.sendMessage(Component.text()
                .append(LegacyUtils.parseLegacy(
                    replace(Lang[Origins.COMMAND_SET_CURRENT],
                        "{PlayerDisplayName}", player.displayName,
                        "{origin}", origin.identity.displayName)))
                .build()
            )
            event.isCancelled = true
            return
        }

        Bukkit.getServer().onlinePlayers.forEach { player1x ->
            if(player1x != event.sender) {
                player1x.sendMessage(Component.text()
                    .append(LegacyUtils.parseLegacy(
                        replace(Lang[Origins.SELECT_BROADCAST],
                            "{PlayerDisplayName}", player.displayName,
                            "{var}", origin.identity.displayName)))
                    .build()
                )
            }
        }
        event.sender?.sendMessage(Component.text()
                .append(LegacyUtils.parseLegacy(
                    replace(Lang[Origins.COMMAND_SET_SUCCESS],
                        "{PlayerDisplayName}", player.displayName,
                        "{var}", origin.identity.displayName)))
                .build()
        )
        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1f, 1f)
        player.currentOrigin = origin
        val burnablePlayers = SunLightRunnable.burnablePlayers
        val rainablePlayers = RainRunnable.rainablePlayers
        val waterablePlayers = WaterRunnable.waterablePlayers
        if(burnablePlayers.contains(event.player)) burnablePlayers.remove(event.player)
        if(rainablePlayers.contains(event.player)) rainablePlayers.remove(event.player)
        if(waterablePlayers.contains(event.player)) waterablePlayers.remove(event.player)
        if(origin.enable.sun) burnablePlayers.add(event.player)
        if(origin.enable.rain) rainablePlayers.add(event.player)
        if(origin.enable.water) waterablePlayers.add(event.player)
        PlayerManager[player.uniqueId].save()
        OriginManager.refreshAll(player, origin)
    }

    @EventHandler(priority = EventPriority.NORMAL)
    suspend fun onOriginReset(event: OriginResetEvent) {
        if (event.isCancelled) return
        val player = event.player
        if (player.currentOrigin == null) {
            event.isCancelled = true
            event.sender.sendMessage(Component.text()
                .append(LegacyUtils.parseLegacy(
                    replace(Lang[Origins.COMMAND_GET_NULL],
                    "{PlayerDisplayName}", player.displayName))
                )
            )
            return
        }

        if(event.sender != player) {
            player.sendMessage(Component.text()
                .append(LegacyUtils.parseLegacy(
                    replace(Lang[Origins.COMMAND_RESET_TARGET],
                        "{SenderDisplayName}", event.sender.displayName)
                ))
            )
        }
        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1f, 0.7f)
        event.sender.sendMessage(Component.text()
            .append(LegacyUtils.parseLegacy(
                replace(Lang[Origins.COMMAND_RESET_SENDER],
                    "{PlayerDisplayName}", player.displayName,))).build()
        )
        player.currentOrigin = null
        PlayerManager[player.uniqueId].save()
        OriginManager.removeAll(player)
    }
}