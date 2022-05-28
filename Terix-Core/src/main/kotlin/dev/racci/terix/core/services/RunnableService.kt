package dev.racci.terix.core.services

import com.github.benmanes.caffeine.cache.Caffeine
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.events.LiquidType
import dev.racci.minix.api.events.LiquidType.Companion.liquidType
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.isDay
import dev.racci.minix.api.extensions.player
import dev.racci.minix.api.extensions.sync
import dev.racci.minix.api.scheduler.CoroutineScheduler
import dev.racci.minix.api.utils.kotlin.and
import dev.racci.minix.api.utils.kotlin.ifTrue
import dev.racci.minix.api.utils.unsafeCast
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.Terix
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.extensions.playSound
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.core.extensions.inDarkness
import dev.racci.terix.core.extensions.inRain
import dev.racci.terix.core.extensions.inSunlight
import dev.racci.terix.core.extensions.inWater
import dev.racci.terix.core.extensions.origin
import dev.racci.terix.core.extensions.wasInDarkness
import dev.racci.terix.core.extensions.wasInRain
import dev.racci.terix.core.extensions.wasInSunlight
import dev.racci.terix.core.extensions.wasInWater
import dev.racci.terix.core.origins.invokeAdd
import dev.racci.terix.core.origins.invokeRemove
import dev.racci.terix.core.services.runnables.AmbientTick
import dev.racci.terix.core.services.runnables.ChildCoroutineRunnable
import dev.racci.terix.core.services.runnables.MotherCoroutineRunnable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.runBlocking
import net.minecraft.core.BlockPos
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import kotlin.properties.Delegates
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation

@MappedExtension(Terix::class, "Runnable Service", [OriginService::class, HookService::class])
class RunnableService(override val plugin: Terix) : Extension<Terix>() {
    private var functions: PersistentMap<Trigger, KFunction<*>> by Delegates.notNull()

    private val motherRunnables = Caffeine.newBuilder()
        .evictionListener { uuid: UUID?, value: MotherCoroutineRunnable?, cause ->
            log.debug { "Ordering a hitman on the mother of ${value!!.children.size} hideous children who's father was $uuid. (Reason: $cause)" }
            runBlocking { CoroutineScheduler.shutdownTask(value!!.taskID) }
        }.build(::getNewMother)

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

        if (ambientSound != null) { mother.children += AmbientTick(player, ambientSound, mother) }

        mother.children += getTasks(origin).map {
            object : ChildCoroutineRunnable(mother) {
                override suspend fun run() {
                    it.callSuspend(this@RunnableService, player, origin)
                }
            }
        }

        return mother
    }

    internal fun getTasks(origin: AbstractOrigin): HashSet<KFunction<*>> {
        val taskList = hashSetOf<KFunction<*>>()

        registerTask(origin.titles.keys, taskList)
        registerTask(origin.potions.keys, taskList)
        registerTask(origin.damageTicks.keys, taskList)
        registerTask(origin.triggerBlocks.keys, taskList)
        registerTask(origin.attributeModifiers.keys, taskList)

        return taskList
    }

    private fun registerTask(
        triggers: Collection<Trigger>,
        list: HashSet<KFunction<*>>
    ) {
        for (trigger in triggers) {
            val task: Any = when (trigger) {
                Trigger.SUNLIGHT -> functions[Trigger.SUNLIGHT]
                Trigger.WATER -> functions[Trigger.WATER]
                Trigger.RAIN -> functions[Trigger.RAIN]
                Trigger.DARKNESS -> functions[Trigger.DARKNESS]
                Trigger.WET -> functions[Trigger.WATER] and functions[Trigger.RAIN]
                else -> {
                    log.debug { "Non applicable trigger for runnable services: $trigger" }; continue
                }
            }!!

            if (task is PersistentList<*>) {
                list.addAll(task.unsafeCast())
            } else list.add(task.unsafeCast())
        }
    }

    internal suspend fun doInvoke(
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

    // TODO: Convert to individual runnable classes
    @Ticker(Trigger.SUNLIGHT)
    @Suppress("UNUSED")
    internal suspend fun doSunlightTick(
        player: Player,
        origin: AbstractOrigin
    ) {
        val bool = shouldTickSunlight(player)
        doInvoke(player, origin, Trigger.SUNLIGHT, player.wasInSunlight, player.inSunlight)

        if (!bool) return
        val ticks = origin.damageTicks[Trigger.SUNLIGHT] ?: return
        val helmet = player.inventory.helmet

        if (helmet == null) {
            if (player.fireTicks > ticks) return
            player.fireTicks = ticks.toInt()
            return
        }

        HookService.getService()
            .get<HookService.EcoEnchantsHook>()
            ?.sunResistance
            ?.let(helmet::hasEnchant)
            ?.ifTrue { return }

        val nms = player.toNMS()
        val amount = nms.random.nextInt(0, 2).takeIf { it != 0 } ?: return
        if (helmet.damage + amount > helmet.maxItemUseDuration) (helmet as CraftItemStack).handle.hurtAndBreak(amount, nms) {}
        (helmet as CraftItemStack).handle.hurt(amount, nms.level.random, nms)
    }

    @Ticker(Trigger.WATER)
    @Suppress("UNUSED")
    internal suspend fun doWaterTick(
        player: Player,
        origin: AbstractOrigin
    ) {
        player.wasInWater = player.inWater
        player.inWater = player.location.block.type.liquidType != LiquidType.WATER
        doInvoke(player, origin, Trigger.WATER, player.wasInWater, player.inWater)
        if (!player.inWater) return

        val wet = origin.damageTicks[Trigger.WET]
        val water = origin.damageTicks[Trigger.WATER]
        if (wet == null && water == null) return

        // Get the one which isn't null or whichever is higher
        val ticks = if (wet == null) water!! else if (water == null) wet else maxOf(wet, water)

        sync { player.damage(ticks) }
    }

    @Ticker(Trigger.RAIN)
    @Suppress("UNUSED")
    internal suspend fun doRainTick(
        player: Player,
        origin: AbstractOrigin
    ) {
        player.wasInRain = player.inRain
        player.inRain = player.isInRain
        doInvoke(player, origin, Trigger.RAIN, player.wasInRain, player.inRain)
        if (!player.inRain) return

        val wet = origin.damageTicks[Trigger.WET]
        val rain = origin.damageTicks[Trigger.RAIN]
        if (wet == null && rain == null) return

        // Get the one which isn't null or whichever is higher
        val ticks = if (wet == null) rain!! else if (rain == null) wet else maxOf(wet, rain)

        sync { player.damage(ticks) }
    }

    // @Ticker(Trigger.HOT) TODO
    // @Ticker(Trigger.COLD) TODO

    @Ticker(Trigger.DARKNESS)
    @Suppress("UNUSED")
    internal suspend fun doDarknessTick(
        player: Player,
        origin: AbstractOrigin
    ) {
        player.wasInDarkness = player.inDarkness
        player.inDarkness = player.inDarkness()
        doInvoke(player, origin, Trigger.DARKNESS, player.wasInDarkness, player.inDarkness)
        if (!player.inDarkness) return

        val damage = origin.damageTicks[Trigger.DARKNESS] ?: return
        if (false) return // TODO: Implement chance so it doesn't damage 4 times a second

        sync { player.damage(damage) }
    }

    @Suppress("UNUSED")
    internal suspend fun doAmbientTick(
        player: Player,
        origin: AbstractOrigin
    ) {
        val sound = origin.sounds.ambientSound ?: return

        player.location.playSound(
            sound.resourceKey.asString(),
            sound.volume,
            sound.pitch,
            sound.distance,
            player
        )
    }

    internal fun shouldTickSunlight(player: Player): Boolean {
        val nms = player.toNMS()
        val brightness = nms.brightness
        val pos = BlockPos(nms.x, nms.eyeY, nms.z)

        val presentPrevention = player.isInWaterOrRainOrBubbleColumn || player.isInPowderedSnow
        val shouldBurn = { (nms.random.nextFloat() * 15.0f) < ((brightness - 0.4f) * 2.0f) } // Lazy evaluation
        val actuallyInSunlight = player.isDay &&
            !presentPrevention &&
            brightness > 0.5f &&
            nms.level.canSeeSky(pos)

        player.wasInSunlight = player.inSunlight
        player.inSunlight = actuallyInSunlight
        return actuallyInSunlight && shouldBurn()
    }

    init {
        functions = persistentMapOf(
            *this::class.declaredMemberFunctions
                .mapNotNull {
                    val anno = it.findAnnotation<Ticker>() ?: return@mapNotNull null
                    anno.trigger to it
                }.toTypedArray()
        )
    }

    private annotation class Ticker(val trigger: Trigger)
}
