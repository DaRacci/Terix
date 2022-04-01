package dev.racci.terix.core.services

import com.github.benmanes.caffeine.cache.Caffeine
import dev.racci.minix.api.coroutine.launch
import dev.racci.minix.api.events.LiquidType
import dev.racci.minix.api.events.LiquidType.Companion.liquidType
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.player
import dev.racci.minix.api.extensions.scheduler
import dev.racci.minix.api.extensions.ticks
import dev.racci.minix.api.scheduler.CoroutineTask
import dev.racci.minix.api.utils.kotlin.and
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.Terix
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.core.extension.canSeeSky
import dev.racci.terix.core.extension.inDarkness
import dev.racci.terix.core.extension.inRain
import dev.racci.terix.core.extension.inSunlight
import dev.racci.terix.core.extension.inWater
import dev.racci.terix.core.extension.origin
import dev.racci.terix.core.extension.validToBurn
import dev.racci.terix.core.extension.wasInDarkness
import dev.racci.terix.core.extension.wasInRain
import dev.racci.terix.core.extension.wasInSunlight
import dev.racci.terix.core.extension.wasInWater
import dev.racci.terix.core.origins.invokeAdd
import dev.racci.terix.core.origins.invokeRemove
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.inject
import java.util.UUID
import kotlin.math.roundToInt

class RunnableService(override val plugin: Terix) : Extension<Terix>() {
    private val hookService by inject<HookService>()

    override val name = "Runnable Service"
    override val dependencies = persistentListOf(OriginService::class, HookService::class)

    private val tasks = mutableMapOf<UUID, MutableList<suspend (Player, AbstractOrigin) -> Unit>>()

    private val schedules = Caffeine.newBuilder()
        .removalListener<UUID, CoroutineTask> { key, value, _ ->
            log.debug { "Cancelling task for $key." }
            plugin.launch(Dispatchers.Default) { value?.cancel() }
            key?.let { tasks -= it }
        }
        .build<UUID, CoroutineTask> { uuid -> player(uuid)?.refreshTasks() }

    override suspend fun handleEnable() {

        event<PlayerJoinEvent> { schedules[player.uniqueId] }
        event<PlayerQuitEvent> { schedules.invalidate(player.uniqueId) }
        event<PlayerOriginChangeEvent>(ignoreCancelled = true, priority = EventPriority.MONITOR) {
            schedules.getIfPresent(player.uniqueId)?.let {
                log.debug { "Cancelling task for $name." }
                it.cancel()
            }
            player.refreshTasks()
        }
    }

    /*Used to damage the player from an async function since damage must be on the main thread*/
    private fun Player.commitDamage(double: Double) { plugin.launch { damage(double) } }

    private fun Player.refreshTasks(): CoroutineTask? {
        log.debug { "Refreshing tasks for $name." }
        val origin = origin()
        val taskList = mutableListOf<suspend (Player, AbstractOrigin) -> Unit>()

        for (trigger in listOf(Trigger.SUNLIGHT, Trigger.WATER, Trigger.RAIN, Trigger.WET, Trigger.DARKNESS)) {
            if (origin.attributeModifiers[trigger] != null || origin.titles[trigger] != null || origin.potions[trigger] != null || origin.damageTicks[trigger] != null || origin.triggerBlocks[trigger] != null) {
                when (trigger) {
                    Trigger.SUNLIGHT -> {
                        log.debug { "Adding sunlight task for $name." }
                        taskList.add(sunlightTick)
                    }
                    Trigger.WATER -> {
                        log.debug { "Adding water task for $name." }
                        taskList.add(waterTask)
                    }
                    Trigger.RAIN -> {
                        log.debug { "Adding rain task for $name." }
                        taskList.add(rainTask)
                    }
                    Trigger.DARKNESS -> {
                        log.debug { "Adding darkness task for $name." }
                        taskList.add(darknessTask)
                    }
                    else -> {
                        log.debug { "Adding water and rain task for $name." }
                        taskList.addAll(waterTask and rainTask)
                    }
                }
            }
        }

        return if (taskList.isEmpty()) {
            log.debug { "No tasks for ${this.name}." }
            tasks -= uniqueId
            null
        } else {
            tasks[uniqueId] = taskList
            scheduler {
                tasks[uniqueId]?.forEach { it.invoke(this, origin) }
            }.runAsyncTaskTimer(plugin, 5.ticks, 5.ticks)
        }
    }

    private val sunlightTick: suspend (Player, AbstractOrigin) -> Unit = { player, origin ->
        val nms = player.toNMS()
        player.wasInSunlight = player.inSunlight

        if (player.inSunlight()) {
            player.inSunlight = true
            log.debug { "Stage 1: $name is in sunlight." }
            if (player.validToBurn()) {
                log.debug { "Stage 2: $name is valid to burn." }
                origin.damageTicks[Trigger.SUNLIGHT]?.let { ticks ->
                    log.debug { "Stage 3: $name has ticks." }
                    val helmet = player.inventory.helmet
                    if (helmet != null) {
                        if (hookService[HookService.EcoEnchantsHook::class].let { it != null && helmet.enchants.contains(it.sunResistance) }) {
                            log.debug { "Stage 4: $name has a helmet." }
                            helmet.damage += nms.random.nextInt(2)
                        }
                    } else {
                        log.debug { "Stage 5: $name does not have a helmet." }
                        if (player.fireTicks > ticks) return@let
                        log.debug { "Stage 6: fire ticks for $name are now ${ticks.roundToInt()}." }
                        player.fireTicks = ticks.toInt()
                        // runSync { player.toNMS().setSecondsOnFire(ticks.toInt() / 20) }
                    }
                }
            } else if (player.fireTicks == 0) { player.playSound(Sound.sound(Key.key("entity.player.extinguish"), Sound.Source.PLAYER, 1f, 1f)) }
        } else { player.inSunlight = false }

        if (player.wasInSunlight != player.inSunlight) {
            when {
                player.wasInSunlight -> Trigger.SUNLIGHT.invokeRemove(player, origin)
                player.inSunlight -> Trigger.SUNLIGHT.invokeAdd(player, origin)
            }
        }
    }

    private val waterTask: suspend (Player, AbstractOrigin) -> Unit = { player, origin ->
        player.wasInWater = player.inWater

        if (player.location.block.liquidType == LiquidType.WATER) {
            player.inWater = true
            origin.damageTicks[Trigger.WATER]?.let { player.commitDamage(it) }
        } else { player.inSunlight = false }

        if (player.wasInWater != player.inWater) {
            when {
                player.wasInWater -> Trigger.WATER.invokeRemove(player, origin)
                player.inWater -> Trigger.WATER.invokeAdd(player, origin)
            }
        }
    }

    private val rainTask: suspend (Player, AbstractOrigin) -> Unit = { player, origin ->
        player.wasInRain = player.inRain

        if (player.isInRain) {
            player.inRain = true
            origin.damageTicks[Trigger.RAIN]?.let { player.commitDamage(it) }
        } else { player.inRain = false }

        if (player.wasInRain != player.inRain) {
            when {
                player.wasInRain -> Trigger.RAIN.invokeRemove(player, origin)
                player.inRain -> Trigger.RAIN.invokeAdd(player, origin)
            }
        }
    }

    private val darknessTask: suspend (Player, AbstractOrigin) -> Unit = { player, origin ->
        player.wasInDarkness = player.inDarkness

        if (player.inDarkness()) {
            player.inDarkness = true
            origin.damageTicks[Trigger.DARKNESS]?.let { player.commitDamage(it) }
        } else { player.inDarkness = false; log.debug { "${player.name} is not in darkness." } }

        if (player.wasInDarkness != player.inDarkness) {
            when {
                player.wasInDarkness -> Trigger.DARKNESS.invokeRemove(player, origin)
                player.inDarkness -> Trigger.DARKNESS.invokeAdd(player, origin)
            }
        }
    }

    private fun Player.inSunlight() =
        if (world.isDayTime) {
            val nms = toNMS()
            val brightness = nms.brightness > 0.5f
            val wetOrCold = !(isInWaterOrRainOrBubbleColumn || isInPowderedSnow || nms.wasInPowderSnow)
            val canSeeSky = canSeeSky()
            brightness && wetOrCold && canSeeSky
        } else false
}
