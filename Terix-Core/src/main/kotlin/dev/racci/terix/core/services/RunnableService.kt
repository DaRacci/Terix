package dev.racci.terix.core.services

import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.async
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.onlinePlayers
import dev.racci.minix.api.extensions.player
import dev.racci.minix.api.utils.kotlin.and
import dev.racci.minix.api.utils.unsafeCast
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
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

// TODO -> Merge fully into TickService
@MappedExtension(Terix::class, "Runnable Service", [HookService::class, TickService::class])
class RunnableService(override val plugin: Terix) : Extension<Terix>() {
    private val motherRunnables = HashMap<UUID, MotherCoroutineRunnable>()

    // Use native code :)
    external fun getMotherRunnable(player: Player): MotherCoroutineRunnable?

    override suspend fun handleEnable() {
        onlinePlayers.forEach(::addIfNeeded)

        event<PlayerJoinEvent> { addIfNeeded(player) }
        event<PlayerQuitEvent> { motherRunnables.remove(player.uniqueId) }
        event<PlayerOriginChangeEvent>(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true,
            forceAsync = true
        ) { addIfNeeded(player) }

        startFlowListener()
    }

    override suspend fun handleUnload() { motherRunnables.clear() }

    private fun addIfNeeded(player: Player) {
        motherRunnables.compute(player.uniqueId) { uuid, _ -> getNewMother(uuid) }
    }

    private fun startFlowListener() = async {
        val service = TickService.getService()
        service.playerFlow
            .filter { player -> motherRunnables.contains(player.uniqueId) }
            .conflate().collect { player -> motherRunnables[player.uniqueId]!!.run() }
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
                else -> continue
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
