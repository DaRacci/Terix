package dev.racci.terix.core.services

import com.github.benmanes.caffeine.cache.Caffeine
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.player
import dev.racci.minix.api.scheduler.CoroutineScheduler
import dev.racci.minix.api.utils.kotlin.and
import dev.racci.minix.api.utils.ticks
import dev.racci.minix.api.utils.unsafeCast
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.Terix
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.core.extensions.origin
import dev.racci.terix.core.origins.invokeAdd
import dev.racci.terix.core.origins.invokeRemove
import dev.racci.terix.core.services.runnables.AmbientTick
import dev.racci.terix.core.services.runnables.DarknessTick
import dev.racci.terix.core.services.runnables.MotherCoroutineRunnable
import dev.racci.terix.core.services.runnables.RainTick
import dev.racci.terix.core.services.runnables.SunlightTick
import dev.racci.terix.core.services.runnables.WaterTick
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.runBlocking
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

@MappedExtension(Terix::class, "Runnable Service", [OriginService::class, HookService::class])
class RunnableService(override val plugin: Terix) : Extension<Terix>() {
    private val motherRunnables =
        Caffeine.newBuilder()
            .removalListener { uuid: UUID?, value: MotherCoroutineRunnable?, cause ->
                log.debug { "Ordering a hitman on the mother of ${value!!.children.size} hideous children who's father was $uuid. (Reason: $cause)" }
                runBlocking { CoroutineScheduler.shutdownTask(value!!.taskID) }
            }
            .build { uuid: UUID ->
                val mother = getNewMother(uuid)
                mother?.runAsyncTaskTimer(plugin, 5.ticks, 5.ticks)
                mother
            }

    override suspend fun handleEnable() {
        event<PlayerJoinEvent> { motherRunnables[player.uniqueId] }
        event<PlayerQuitEvent> { motherRunnables.invalidate(player.uniqueId) }
        event<PlayerOriginChangeEvent>(ignoreCancelled = true, priority = EventPriority.MONITOR) { motherRunnables.refresh(player.uniqueId) }
    }

    override suspend fun handleUnload() {
        motherRunnables.invalidateAll()
    }

    private fun getNewMother(uuid: UUID): MotherCoroutineRunnable? {
        val mother = MotherCoroutineRunnable()
        val player = player(uuid) ?: return null
        val origin = player.origin()
        val ambientSound = origin.sounds.ambientSound

        if (ambientSound != null) { AmbientTick(player, ambientSound, this@RunnableService, mother) }

        attachChildren(mother, player, origin)

        return mother
    }

    private fun attachChildren(
        mother: MotherCoroutineRunnable,
        player: Player,
        origin: AbstractOrigin
    ) {
        registerTask(player, origin, origin.titles.keys, mother)
        registerTask(player, origin, origin.potions.keys, mother)
        registerTask(player, origin, origin.damageTicks.keys, mother)
        registerTask(player, origin, origin.triggerBlocks.keys, mother)
        registerTask(player, origin, origin.attributeModifiers.keys, mother)
    }

    private fun registerTask(
        player: Player,
        origin: AbstractOrigin,
        triggers: Collection<Trigger>,
        mother: MotherCoroutineRunnable
    ) {
        for (trigger in triggers) {
            val task: Any = when (trigger) {
                Trigger.SUNLIGHT -> SunlightTick::class
                Trigger.WATER -> WaterTick::class
                Trigger.RAIN -> RainTick::class
                Trigger.DARKNESS -> DarknessTick::class
                Trigger.WET -> WaterTick::class and RainTick::class
                else -> {
                    log.debug { "Non applicable trigger for runnable services: $trigger" }
                    continue
                }
            }

            if (task is PersistentList<*>) {
                task.forEach { checkAndRegister(player, origin, mother, it) }
            } else checkAndRegister(player, origin, mother, task)
        }
    }

    private fun checkAndRegister(
        player: Player,
        origin: AbstractOrigin,
        mother: MotherCoroutineRunnable,
        taskClazz: Any?
    ) {
        if (taskClazz == null) return
        if (mother.children.any { it::class == taskClazz }) return
        taskClazz.unsafeCast<KClass<*>>().primaryConstructor!!.call(
            player,
            origin,
            this@RunnableService,
            mother
        )
    }

    suspend fun doInvoke(
        player: Player,
        origin: AbstractOrigin,
        trigger: Trigger,
        wasBool: Boolean,
        isBool: Boolean
    ) {
        if (wasBool == isBool) return

        if (isBool) {
            trigger.invokeAdd(player, origin)
        } else trigger.invokeRemove(player, origin)
    }

    // @Ticker(Trigger.HOT) TODO
    // @Ticker(Trigger.COLD) TODO
}
