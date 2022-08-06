package dev.racci.terix.core.services

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent
import com.destroystokyo.paper.event.entity.PhantomPreSpawnEvent
import com.destroystokyo.paper.event.entity.ProjectileCollideEvent
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent
import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent
import com.destroystokyo.paper.event.player.PlayerJumpEvent
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.events.PlayerDoubleLeftClickEvent
import dev.racci.minix.api.events.PlayerDoubleOffhandEvent
import dev.racci.minix.api.events.PlayerDoubleRightClickEvent
import dev.racci.minix.api.events.PlayerEnterLiquidEvent
import dev.racci.minix.api.events.PlayerExitLiquidEvent
import dev.racci.minix.api.events.PlayerLeftClickEvent
import dev.racci.minix.api.events.PlayerOffhandEvent
import dev.racci.minix.api.events.PlayerRightClickEvent
import dev.racci.minix.api.events.PlayerShiftDoubleLeftClickEvent
import dev.racci.minix.api.events.PlayerShiftDoubleOffhandEvent
import dev.racci.minix.api.events.PlayerShiftDoubleRightClickEvent
import dev.racci.minix.api.events.PlayerShiftLeftClickEvent
import dev.racci.minix.api.events.PlayerShiftOffhandEvent
import dev.racci.minix.api.events.PlayerShiftRightClickEvent
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.utils.collections.multiMapOf
import dev.racci.minix.api.utils.safeCast
import dev.racci.minix.api.utils.unsafeCast
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.Terix
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.origin
import dev.racci.terix.api.origins.origin.AbstractOrigin
import dev.racci.terix.api.origins.origin.OriginEventListener
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityAirChangeEvent
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.event.entity.EntityDamageByBlockEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityEvent
import org.bukkit.event.entity.EntityResurrectEvent
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.event.entity.EntityToggleSwimEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerBedLeaveEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerRiptideEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.event.player.PlayerToggleSprintEvent
import org.koin.core.component.get
import org.spigotmc.event.entity.EntityDismountEvent
import org.spigotmc.event.entity.EntityMountEvent
import org.spigotmc.event.player.PlayerSpawnLocationEvent
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.valueParameters

// TODO: Only register when used by at-least one active origin.
// TODO: Create a cleaner function to register events.
// TODO -> HashMap reference for events to the KCallable
// TODO -> Implement caching knowledge of if an origin should have the event forwarded.
@MappedExtension(Terix::class, "Event Forwarder Service", [OriginService::class])
class EventForwarderService(override val plugin: Terix) : Extension<Terix>() {
    private val events = multiMapOf<KClass<out Event>, suspend (Event) -> Unit>()
    private val functionCache = mutableMapOf<KClass<out Event>, KFunction<Event>>()

    override suspend fun handleEnable() {
        this.registerComboEvents()
        this.registerUseEvents()
        this.registerToggleEvents()
        this.registerSpawnEvents()
        this.registerMovementEvents()
        this.registerProjectileEvents()
        this.registerDeathEvents()
        this.registerDamageEvents()
        this.registerBlockEvents()
        this.registerInventoryEvents()
        this.registerMiscellaneousEvents()

        this.activateListeners()
    }

    private fun registerComboEvents() {
        this.registerPlayerEvent<PlayerLeftClickEvent>()
        this.registerPlayerEvent<PlayerRightClickEvent>()
        this.registerPlayerEvent<PlayerOffhandEvent>()
        this.registerPlayerEvent<PlayerDoubleLeftClickEvent>()
        this.registerPlayerEvent<PlayerDoubleRightClickEvent>()
        this.registerPlayerEvent<PlayerDoubleOffhandEvent>()
        this.registerPlayerEvent<PlayerShiftLeftClickEvent>("onSneakLeftClick")
        this.registerPlayerEvent<PlayerShiftRightClickEvent>("onSneakRightClick")
        this.registerPlayerEvent<PlayerShiftOffhandEvent>("onSneakOffhand")
        this.registerPlayerEvent<PlayerShiftDoubleLeftClickEvent>("onSneakDoubleLeftClick")
        this.registerPlayerEvent<PlayerShiftDoubleRightClickEvent>("onSneakDoubleRightClick")
        this.registerPlayerEvent<PlayerShiftDoubleOffhandEvent>("onSneakDoubleOffhand")
    }

    private fun registerUseEvents() {
        this.registerPlayerEvent<PlayerElytraBoostEvent>()
        this.registerPlayerEvent<PlayerRiptideEvent>()
        this.registerPlayerEvent<PlayerFishEvent>()
        this.registerPlayerEvent<PlayerInteractEvent>()
        this.registerPlayerEvent<PlayerBedEnterEvent>()
        this.registerPlayerEvent<PlayerBedLeaveEvent>()
        this.registerPlayerEvent<PlayerItemConsumeEvent>()
        this.registerPlayerEvent<PlayerItemDamageEvent>()
    }

    private fun registerToggleEvents() {
        this.registerEntityEvent<EntityToggleSwimEvent>()
        this.registerEntityEvent<EntityToggleGlideEvent>()
        this.registerPlayerEvent<PlayerToggleSneakEvent>()
        this.registerPlayerEvent<PlayerToggleSprintEvent>()
    }

    private fun registerMovementEvents() {
        this.registerPlayerEvent<PlayerJumpEvent>()
        this.registerPlayerEvent<PlayerExitLiquidEvent>()
        this.registerPlayerEvent<PlayerEnterLiquidEvent>()
        this.registerPlayerEvent<PlayerChangedWorldEvent>()
        this.registerEntityEvent<EntityMountEvent>()
        this.registerEntityEvent<EntityDismountEvent>()
    }

    private fun registerSpawnEvents() {
        this.registerPlayerEvent<PlayerRespawnEvent>()
        this.registerPlayerEvent<PlayerPostRespawnEvent>()
        this.registerPlayerEvent<PlayerSpawnLocationEvent>()
        this.registerEntityEvent<EntityResurrectEvent>()
        this.finaliseEvent<PhantomPreSpawnEvent>("PhantomSpawn") { it.spawningEntity as? Player }
    }

    private fun registerProjectileEvents() {
        this.registerPlayerEvent<PlayerLaunchProjectileEvent>()
        this.finaliseEvent<ProjectileHitEvent>(null) { it.entity.shooter as? Player }
        this.finaliseEvent<ProjectileCollideEvent>(null) { it.entity.shooter as? Player }
    }

    private fun registerDeathEvents() {
        this.finaliseEvent(null, null, PlayerDeathEvent::getPlayer)
        this.finaliseEvent<EntityDeathEvent>("KillEntity") { it.entity.killer }
    }

    private fun registerDamageEvents() {
        this.registerEntityEvent<EntityDamageEvent>()
        this.registerEntityEvent<EntityDamageByEntityEvent>()
        this.registerEntityEvent<EntityCombustEvent>()
        this.registerEntityEvent<EntityKnockbackByEntityEvent>("KnockbackByEntity")
        this.registerEntityEvent<EntityDamageByBlockEvent>("DamageByBlock")
        this.finaliseEvent<EntityKnockbackByEntityEvent>("KnockbackEntity") { it.hitBy as? Player }
        this.finaliseEvent<EntityDamageByEntityEvent>("DamageEntity") { it.damager as? Player }
    }

    private fun registerBlockEvents() {
        this.finaliseEvent(null, null, BlockBreakEvent::getPlayer)
        this.finaliseEvent(null, null, BlockPlaceEvent::getPlayer)
    }

    private fun registerInventoryEvents() {
        this.finaliseEvent<InventoryOpenEvent>(null) { it.player as? Player }
        this.registerPlayerEvent<PlayerArmorChangeEvent>()
    }

    private fun registerMiscellaneousEvents() {
        this.finaliseEvent("BecomeOrigin", PlayerOriginChangeEvent::newOrigin, null)
        this.finaliseEvent("ChangeOrigin", PlayerOriginChangeEvent::preOrigin, null)
        this.registerEntityEvent<EntityAirChangeEvent>()
    }

    private fun activateListeners() {
        val origins = get<OriginService>().getOrigins().values

        for (origin in origins) {
            for ((clazz, events) in events.entries) {
                if (events == null) continue

                this.getForwarding(origin, clazz) ?: continue

                // Register the event
                origin.event(
                    type = clazz,
                    plugin = plugin,
                    priority = EventPriority.LOWEST
                ) {
                    events.forEach { it(this) }
                }
            }
        }
    }

    private fun <E : Event> getForwarding(
        listener: OriginEventListener,
        eventKClass: KClass<E>
    ): KFunction<E>? {
        val function = functionCache[eventKClass]

        if (function == null) {
            plugin.log.debug { "${this::class.simpleName} doesn't override the event ${eventKClass.simpleName}" }
            return null
        }

        if (function.valueParameters.size > 1) {
            plugin.log.debug { "${listener::class.simpleName} overrides the event ${eventKClass.simpleName} with more than one parameter" }
            return null
        }

        return function.unsafeCast()
    }

    //    private inline fun <reified T : PlayerEvent> registerPlayerEvent(
//        function: KFunction<Unit>
//    ) = this.finaliseEvent<T>(function.name, null, PlayerEvent::getPlayer)
//
    private inline fun <reified T : PlayerEvent> registerPlayerEvent(name: String? = null) {
        this.finaliseEvent<T>(name, null, PlayerEvent::getPlayer)
    }

    private inline fun <reified T : EntityEvent> registerEntityEvent(name: String? = null) {
        this.finaliseEvent<T>(name) { it.entity as? Player }
    }

    private inline fun <reified E : Event> finaliseEvent(
        name: String?,
        noinline originCallback: ((E) -> AbstractOrigin?)? = null,
        noinline playerCallback: ((E) -> Player?)?
    ) {
        val function = this.getFunction<E>(name) ?: return
        log.debug { "Registering event: ${E::class.qualifiedName} -> ${function.name}" }

        val callback: suspend (Event) -> Unit = when {
            originCallback == null && playerCallback != null -> {
                call@{
                    val player = playerCallback(it as E).safeCast<Player>() ?: return@call
                    val origin = origin(player)
                    function.call(origin, it)
                }
            }

            playerCallback == null && originCallback != null -> {
                {
                    val origin = originCallback(it as E)
                    function.call(origin, it)
                }
            }

            else -> null
        } ?: return

        events.put(E::class, callback)
    }

    private inline fun <reified T : Event> getFunction(name: String?): KFunction<T>? {
        if (this.functionCache[T::class] != null) return this.functionCache[T::class].unsafeCast()

        var nonNullName = name
        if (nonNullName == null) {
            val fromFunction = T::class.simpleName
                ?.removePrefix("Entity")
                ?.removePrefix("Player")
                ?.removeSuffix("Event")

            nonNullName = "on$fromFunction"
        }

        if (nonNullName.isEmpty() || nonNullName == "on") {
            log.debug { "No event name for ${T::class.simpleName}" }
            return null
        }

        val itr = AbstractOrigin::class.memberFunctions.iterator()
        while (itr.hasNext()) {
            val next = itr.next()
            if (next.name != nonNullName || next.valueParameters[0].type != T::class.starProjectedType) continue

            this.functionCache[T::class] = next.unsafeCast()
            break
        }

        return this.functionCache[T::class].safeCast()
    }
}
