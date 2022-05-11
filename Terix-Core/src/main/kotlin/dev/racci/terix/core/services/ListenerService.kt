package dev.racci.terix.core.services

import com.destroystokyo.paper.event.block.BeaconEffectEvent
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.events.PlayerEnterLiquidEvent
import dev.racci.minix.api.events.PlayerExitLiquidEvent
import dev.racci.minix.api.events.WorldDayEvent
import dev.racci.minix.api.events.WorldNightEvent
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.inOverworld
import dev.racci.minix.api.extensions.onlinePlayers
import dev.racci.minix.api.services.DataService
import dev.racci.minix.api.utils.kotlin.invokeIfNotNull
import dev.racci.terix.api.Terix
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.api.origins.enums.Trigger.Companion.getTrigger
import dev.racci.terix.core.data.Lang
import dev.racci.terix.core.data.PlayerData
import dev.racci.terix.core.extension.fromOrigin
import dev.racci.terix.core.extension.invokeIfPresent
import dev.racci.terix.core.extension.message
import dev.racci.terix.core.extension.origin
import dev.racci.terix.core.origins.invokeAdd
import dev.racci.terix.core.origins.invokeBase
import dev.racci.terix.core.origins.invokeCompleteClean
import dev.racci.terix.core.origins.invokeReload
import dev.racci.terix.core.origins.invokeRemovalFor
import dev.racci.terix.core.origins.invokeRemove
import dev.racci.terix.core.origins.invokeSwap
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.potion.PotionEffect
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.get

// Check for button presses to invoke actions in the test chambers
@MappedExtension(Terix::class, "Listener Service")
class ListenerService(override val plugin: Terix) : Extension<Terix>() {
    private val lang by get<DataService>().inject<Lang>()

    override suspend fun handleEnable() {
        event<BeaconEffectEvent> {
            if (player.getPotionEffect(effect.type)?.fromOrigin() == true) { cancel(); return@event }
            effect = PotionEffect(
                effect.type,
                effect.duration,
                effect.amplifier,
                effect.isAmbient,
                false,
                false
            )
        }

        event<AsyncPlayerPreLoginEvent> {
            transaction { PlayerData.findById(this@event.uniqueId) ?: PlayerData.new(this@event.uniqueId) {} }
        }

        event<PlayerJoinEvent>(forceAsync = true) {

            val origin = player.origin()
            val activeTriggers = player.activeTriggers().map { it.name.lowercase() }

            val potions = mutableListOf<PotionEffectType>()
            if (origin.nightVision && player.nightVision.name.lowercase() !in activeTriggers) potions.add(PotionEffectType.NIGHT_VISION)
            for (potion in player.activePotionEffects) {
                val key = potion.key?.asString() ?: continue
                val match = PotionEffectBuilder.regex.find(key)?.groups?.get("trigger") ?: continue
                if (match.value in activeTriggers) continue

                potions += potion.type
            }
            ensureMainThread { potions.forEach(player::removePotionEffect) }
            removeUnfulfilledAttributes(player, activeTriggers)

            // Trigger.invokeBase(player) // I don't think this is needed anymore
            // player.world.environment.getTrigger().invokeAdd(player) // Also may not be needed
            delay(0.5.seconds)
            player.sendHealthUpdate()
        }

        event<PlayerPostRespawnEvent>(forceAsync = true) {
            removeUnfulfilledAttributes(player)
            Trigger.invokeBase(player)
            player.world.environment.getTrigger().invokeAdd(player)
            player.health = player.maxHealth
        }

        event<EntityPotionEffectEvent> {
            if (entity is Player &&
                cause == EntityPotionEffectEvent.Cause.MILK &&
                oldEffect != null && oldEffect!!.hasKey() &&
                oldEffect!!.key!!.asString().matches(PotionEffectBuilder.regex)
            ) {
                log.debug { "Canceling milk potion removal for ${entity.name}." }
                cancel()
            }
        }

        event<PlayerEnterLiquidEvent> { Trigger.values().find { it.name == newType.name }?.invokeAdd(player) }
        event<PlayerExitLiquidEvent> { Trigger.values().find { it.name == newType.name }?.invokeRemove(player) }

        event<WorldNightEvent>(forceAsync = true) {
            onlinePlayers.filter { it.inOverworld() } // Time events are only relevant in overworld
                .forEach { player ->
                    val origin = player.origin()

                    Trigger.NIGHT.invokeSwap(Trigger.DAY, player, origin)
                    origin.titles[Trigger.NIGHT]?.invoke(player)
                }
        }

        event<WorldDayEvent>(forceAsync = true) {
            onlinePlayers.filter { it.inOverworld() } // Time events are only relevant in overworld
                .forEach { player ->
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

        event<PlayerOriginChangeEvent>(
            ignoreCancelled = true,
            priority = EventPriority.HIGH
        ) {
            newSuspendedTransaction {
                PlayerData[player.uniqueId].origin = newOrigin
            }
            Trigger.invokeReload(player)

            lang.origin.broadcast[
                "player" to { player.displayName() },
                "new_origin" to { newOrigin.displayName },
                "old_origin" to { preOrigin.displayName }
            ] message onlinePlayers

            newOrigin.becomeOriginTitle?.invoke(player)
        }

        event<EntityCombustEvent> {
            log.debug { "Combust event for ${entity.name}" }
        }
    }
}
