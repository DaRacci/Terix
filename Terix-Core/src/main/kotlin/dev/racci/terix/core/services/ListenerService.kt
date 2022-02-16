package dev.racci.terix.core.services

import com.destroystokyo.paper.MaterialTags
import com.destroystokyo.paper.event.block.BeaconEffectEvent
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import dev.racci.minix.api.events.PlayerEnterLiquidEvent
import dev.racci.minix.api.events.PlayerExitLiquidEvent
import dev.racci.minix.api.events.WorldDayEvent
import dev.racci.minix.api.events.WorldNightEvent
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.onlinePlayers
import dev.racci.minix.api.extensions.ticks
import dev.racci.minix.api.utils.kotlin.ifTrue
import dev.racci.minix.api.utils.kotlin.invokeIfNotNull
import dev.racci.terix.api.Terix
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.api.origins.enums.Trigger.Companion.getTrigger
import dev.racci.terix.core.extension.fromOrigin
import dev.racci.terix.core.extension.invokeIfPresent
import dev.racci.terix.core.extension.origin
import dev.racci.terix.core.origins.invokeAdd
import dev.racci.terix.core.origins.invokeBase
import dev.racci.terix.core.origins.invokeCompleteClean
import dev.racci.terix.core.origins.invokeReload
import dev.racci.terix.core.origins.invokeRemovalFor
import dev.racci.terix.core.origins.invokeRemove
import dev.racci.terix.core.origins.invokeSwap
import dev.racci.terix.core.storage.PlayerData
import kotlinx.coroutines.delay
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockFadeEvent
import org.bukkit.event.entity.EntityAirChangeEvent
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.get
import kotlin.time.ExperimentalTime

// Check for button presses to invoke actions in the test chambers
@OptIn(ExperimentalTime::class)
class ListenerService(override val plugin: Terix) : Extension<Terix>() {

    override val name = "Listener Service"

    override suspend fun handleEnable() {
        event<BeaconEffectEvent> {
            effect = PotionEffect(
                effect.type,
                effect.duration,
                effect.amplifier,
                effect.isAmbient,
                false,
                false
            )
        }

        event<PlayerDropItemEvent>(
            priority = EventPriority.HIGH,
            ignoreCancelled = true,
            forceAsync = true
        ) {
            if (!MaterialTags.CONCRETE_POWDER.isTagged(itemDrop.itemStack)) return@event

            var lastLocation = itemDrop.location
            delay(1.ticks)

            while (itemDrop.location != lastLocation) {
                lastLocation = itemDrop.location
                if (itemDrop.isInWater) {
                    val concreteType = itemDrop.itemStack.type.name.split('_').takeWhile { s -> s != "POWDER" }.joinToString("_")
                    itemDrop.itemStack.type = Material.valueOf(concreteType)
                    break
                }
                delay(2.ticks)
            }
        }

        event<BlockFadeEvent>(
            priority = EventPriority.HIGH,
            ignoreCancelled = true
        ) {
            if (!block.type.name.contains("CORAL") || !newState.type.name.startsWith("DEAD")) return@event
            get<HookService>().get<HookService.LandsHook>()?.integration?.getLand(block.location)?.let { land ->
                if (land.name == "Spawn") {
                    log.debug { "Coral block at ${block.location} is in spawn, cancelling BlockFadeEvent" }
                    cancel()
                }
            }
        }

        event<AsyncPlayerPreLoginEvent> {
            transaction {
                log.debug { "Player $id is logging in, Loading / Creating data." }
                PlayerData.findById(this@event.uniqueId) ?: PlayerData.new(this@event.uniqueId) {}
            }
        }

        event<PlayerJoinEvent>(forceAsync = true) {
            Trigger.invokeCompleteClean(player)
            Trigger.invokeBase(player)
            player.world.environment.getTrigger().invokeAdd(player)
        }

        event<PlayerPostRespawnEvent>(forceAsync = true) {
            Trigger.invokeCompleteClean(player)
            Trigger.invokeBase(player)
            player.world.environment.getTrigger().invokeAdd(player)
        }

        // TODO: Remove only the non origin related potions
        event<EntityPotionEffectEvent> {
            if (entity is Player &&
                cause == EntityPotionEffectEvent.Cause.MILK &&
                oldEffect != null && oldEffect!!.hasKey() &&
                oldEffect!!.key!!.namespace == "origin"
            ) {
                log.debug { "Cancelling milk potion removal for ${entity.name}." }
                cancel()
            }
        }

        event<PlayerEnterLiquidEvent> {
            Trigger.values().find { it.name == newType.name }?.invokeAdd(player)
        }

        event<PlayerExitLiquidEvent> {
            Trigger.values().find { it.name == newType.name }?.invokeRemove(player)
        }

        event<WorldNightEvent>(forceAsync = true) {
            for (player in onlinePlayers) {
                if (player.world.environment != World.Environment.NORMAL) continue
                val origin = player.origin()
                Trigger.NIGHT.invokeSwap(Trigger.DAY, player, origin)
                origin.titles[Trigger.NIGHT]?.invoke(player)
                PlayerData[player].nightVision.takeIf { it == Trigger.NIGHT && player.getPotionEffect(PotionEffectType.NIGHT_VISION).fromOrigin() }?.let {
                    log.debug { "Player ${player.name} needs night vision, Adding it." }
                    player.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false))
                }
            }
        }

        event<WorldDayEvent>(forceAsync = true) {
            for (player in onlinePlayers) {
                if (player.world.environment != World.Environment.NORMAL) continue
                val origin = player.origin()
                Trigger.DAY.invokeSwap(Trigger.NIGHT, player, origin)
                origin.titles[Trigger.DAY]?.invoke(player)
            }
        }

        event<PlayerChangedWorldEvent>(forceAsync = true) {
            val fromTrigger = from.environment.getTrigger()
            val toTrigger = player.world.environment.getTrigger()
            if (fromTrigger == toTrigger) return@event
            if (fromTrigger.ordinal == 4 && toTrigger.ordinal != 4) {
                Trigger.invokeRemovalFor(player, Trigger.DAY, Trigger.NIGHT)
            }
            toTrigger.invokeSwap(fromTrigger, player)
            player.origin().titles[toTrigger]?.invoke(player)
        }

        event<EntityDamageEvent>(
            ignoreCancelled = true,
            priority = EventPriority.LOWEST
        ) {
            val player = entity as? Player ?: return@event
            cause.invokeIfPresent(this, player) { _, multi ->
                if (multi == 0.0) {
                    log.debug { "Cancelling damage for ${player.name} due to $cause" }
                    cancel()
                }
                val new = damage * multi
                log.debug { "Damage for ${player.name} change from $damage to $new due to $cause" }
                damage = new
            }
        }

        event<EntityCombustEvent>(
            ignoreCancelled = true,
            priority = EventPriority.LOWEST
        ) {
            val player = entity as? Player ?: return@event
            if (player.origin().damageMultipliers[EntityDamageEvent.DamageCause.FIRE] == 0.0) {
                log.debug { "Cancelling fire damage for ${player.name} due to multi being 0.0" }
                cancel()
            }
        }

        event<EntityAirChangeEvent>(
            ignoreCancelled = true,
            priority = EventPriority.LOWEST
        ) {
            val player = entity as? Player ?: return@event
            player.origin().waterBreathing.ifTrue(::cancel)
        }

        event<FoodLevelChangeEvent>(
            ignoreCancelled = true,
            priority = EventPriority.LOWEST
        ) {
            val player = entity as? Player ?: return@event
            val item = item ?: return@event
            val origin = player.origin()
            // TODO: Test if this cancels the whole event of just the food change
            origin.foodMultipliers[item.type]?.invokeIfNotNull {
                if (it == 0) {
                    log.debug { "Cancelling food change for ${player.name} due to multi being 0.0" }
                    cancel()
                }
                val new = foodLevel * it
                log.debug { "Food change for ${player.name} change from $foodLevel to $new due to $item" }
                foodLevel = new
            }
            origin.foodAttributes[item.type]?.forEach { it.invoke(player) }
            origin.foodPotions[item.type]?.invokeIfNotNull(player::addPotionEffects)
        }

        event<PlayerOriginChangeEvent> {
            newSuspendedTransaction {
                PlayerData[player.uniqueId].origin = newOrigin
            }
            Trigger.invokeReload(player)
        }
    }
}
