package me.racci.sylphia.listeners

import com.github.shynixn.mccoroutine.asyncDispatcher
import kotlinx.coroutines.withContext
import me.racci.raccicore.utils.extensions.KotlinListener
import me.racci.raccicore.utils.strings.colour
import me.racci.sylphia.Sylphia
import me.racci.sylphia.origins.OriginManager
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerChangedWorldEvent

class PlayerChangeWorldListener : KotlinListener {

    @EventHandler(priority = EventPriority.NORMAL)
    suspend fun onChangeWorld(event: PlayerChangedWorldEvent) = withContext(Sylphia.instance.asyncDispatcher) {
        if(OriginManager.getOrigin(event.player.uniqueId) == null) return@withContext
        OriginManager.refreshAll(event.player)
        event.player.playSound(event.player.location, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f)
        event.player.sendTitle(colour("&cRelease.."), colour("&4You feel power flowing through you."), 10, 60, 10)
    }
}