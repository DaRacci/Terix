package dev.racci.terix.core.services

import com.github.benmanes.caffeine.cache.Caffeine
import dev.racci.minix.api.coroutine.launch
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.scheduler
import dev.racci.minix.api.extensions.ticks
import dev.racci.minix.api.scheduler.CoroutineTask
import dev.racci.terix.api.Terix
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.core.extension.canSeeSky
import dev.racci.terix.core.extension.inDarkness
import dev.racci.terix.core.extension.origin
import dev.racci.terix.core.extension.validToBurn
import kotlinx.collections.immutable.persistentListOf
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.time.Duration
import java.util.UUID

class RunnableService(override val plugin: Terix) : Extension<Terix>() {

    override val name = "Runnable Service"
    override val dependencies get() = persistentListOf(OriginService::class)

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(1))
        .build { player: Player ->
            val origin = player.origin()
            arrayListOf(
                origin.damageTicks[Trigger.SUNLIGHT],
                origin.damageTicks[Trigger.WATER],
                origin.damageTicks[Trigger.DARKNESS]
            )
        }

    override suspend fun handleEnable() {
        val schedules: MutableMap<UUID, CoroutineTask> = mutableMapOf()

        event<PlayerOriginChangeEvent> { cache.refresh(player) }

        event<PlayerJoinEvent> {
            val data = cache[player]
            schedules[player.uniqueId] = scheduler {
                if (player.canSeeSky()) {
                    if (data[0] != null && player.validToBurn()) {
                        player.fireTicks = 100
                    } else if (data[1] != null && player.isInWaterOrRainOrBubbleColumn) {
                        player.commitDamage(data[1]!!)
                    }
                } else if (data[2] != null && player.inDarkness()) {
                    player.commitDamage(data[2]!!)
                }
            }.runAsyncTaskTimer(plugin, 2.ticks, 1.ticks)
        }

        event<PlayerQuitEvent> { schedules[player.uniqueId]?.cancel() }
    }

    /*Used to damage the player from an async function since damage must be on the main thread*/
    private fun Player.commitDamage(double: Double) { plugin.launch { damage(double) } }
}
