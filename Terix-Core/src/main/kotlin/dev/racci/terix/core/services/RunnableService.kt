package dev.racci.terix.core.services

import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.onlinePlayers
import dev.racci.minix.api.extensions.player
import dev.racci.terix.api.Terix
import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.states.State
import dev.racci.terix.core.services.runnables.AmbientTick
import dev.racci.terix.core.services.runnables.ChildCoroutineRunnable
import dev.racci.terix.core.services.runnables.DarknessTick
import dev.racci.terix.core.services.runnables.MotherCoroutineRunnable
import dev.racci.terix.core.services.runnables.RainTick
import dev.racci.terix.core.services.runnables.SunlightTick
import dev.racci.terix.core.services.runnables.WaterTick
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
@MappedExtension(Terix::class, "Runnable Service", [TickService::class], threadCount = 4)
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
        val mother = MotherCoroutineRunnable(supervisor, dispatcher)
        val player = player(uuid) ?: return null
        val origin = TerixPlayer.cachedOrigin(player)
        val ambientSound = origin.sounds.ambientSound

        if (ambientSound != null) { AmbientTick(player, origin, mother, ambientSound) }

        attachChildren(mother, player, origin)

        return mother
    }

    private fun attachChildren(
        mother: MotherCoroutineRunnable,
        player: Player,
        origin: Origin
    ) {
        registerTask(player, origin, origin.stateTitles.keys, mother)
        registerTask(player, origin, origin.statePotions.keys, mother)
        registerTask(player, origin, origin.stateDamageTicks.keys, mother)
        registerTask(player, origin, origin.stateBlocks.keys, mother)
        registerTask(player, origin, origin.attributeModifiers.keys, mother)
    }

    private fun registerTask(
        player: Player,
        origin: Origin,
        triggers: Collection<State>,
        mother: MotherCoroutineRunnable
    ) {
        for (trigger in triggers) {
            val task = when (trigger) {
                State.LightState.SUNLIGHT -> SunlightTick::class
                State.LiquidState.WATER -> WaterTick::class
                State.WeatherState.RAIN -> RainTick::class
                State.LightState.DARKNESS -> DarknessTick::class
                else -> continue
            }

            checkAndRegister(player, origin, mother, task)
        }
    }

    private fun checkAndRegister(
        player: Player,
        origin: Origin,
        mother: MotherCoroutineRunnable,
        taskClazz: KClass<out ChildCoroutineRunnable>
    ) {
        if (mother.children.any { it::class == taskClazz }) return

        logger.debug { "Registering ${taskClazz.simpleName} for ${player.name}" }

        taskClazz.primaryConstructor!!.call(
            player,
            origin,
            this@RunnableService,
            mother
        )
    }

    suspend fun tryToggle(
        player: Player,
        origin: Origin,
        state: State,
        wasBool: Boolean,
        isBool: Boolean
    ) {
        if (wasBool == isBool) return

        if (isBool) {
            state.activate(player, origin)
        } else state.deactivate(player, origin)
    }
}
