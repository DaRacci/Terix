package me.racci.sylphia.listeners

import com.github.shynixn.mccoroutine.asyncDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.racci.raccicore.utils.extensions.KotlinListener
import me.racci.sylphia.Sylphia
import me.racci.sylphia.origins.OriginManager
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerItemConsumeEvent

class PlayerConsumeListener : KotlinListener {

    @EventHandler(priority = EventPriority.HIGH)
    suspend fun onConsume(event: PlayerItemConsumeEvent) = withContext(Sylphia.instance.asyncDispatcher) {
        if (event.isCancelled || OriginManager.getOrigin(event.player.uniqueId) == null) {
            return@withContext
        }
        if (event.item.type == Material.MILK_BUCKET) {
            delay(1000L)
            OriginManager.refreshAll(event.player)
        }
    }

}