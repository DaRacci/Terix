package dev.racci.terix.core.services

import com.destroystokyo.paper.event.block.BeaconEffectEvent
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.collections.PlayerMap
import dev.racci.minix.api.events.keybind.PlayerSecondaryEvent
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.collections.computeAndRemove
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.hasPermissionOrStar
import dev.racci.minix.api.extensions.onlinePlayers
import dev.racci.minix.api.extensions.pdc
import dev.racci.minix.api.extensions.scheduler
import dev.racci.minix.api.flow.eventFlow
import dev.racci.minix.api.services.DataService
import dev.racci.minix.api.services.DataService.Companion.inject
import dev.racci.minix.api.utils.now
import dev.racci.minix.api.utils.ticks
import dev.racci.minix.nms.aliases.NMSItemStack
import dev.racci.minix.nms.aliases.NMSServerPlayer
import dev.racci.terix.api.Terix
import dev.racci.terix.api.data.Lang
import dev.racci.terix.api.data.TerixConfig
import dev.racci.terix.api.data.player.TerixPlayer
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.extensions.handle
import dev.racci.terix.api.extensions.playSound
import dev.racci.terix.api.origins.OriginHelper
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.origin.PlayerLambda
import dev.racci.terix.api.origins.sounds.SoundEffect
import dev.racci.terix.api.origins.sounds.SoundEffects
import dev.racci.terix.api.services.StorageService
import dev.racci.terix.core.commands.TerixPermissions
import dev.racci.terix.core.extensions.fromOrigin
import dev.racci.terix.core.extensions.message
import dev.racci.terix.core.origins.DragonOrigin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.newCoroutineContext
import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.food.FoodProperties
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerGameModeChangeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.koin.core.component.inject
import java.util.UUID
import kotlin.reflect.KProperty1

// TODO -> This class is disgusting, needs to be cleaned up
@MappedExtension(Terix::class, "Listener Service", [DataService::class])
public class ListenerService(override val plugin: Terix) : Extension<Terix>() {
    private val terixConfig by inject<DataService>().inject<TerixConfig>()
    private val lang by inject<DataService>().inject<Lang>()
    private val finishEventAction: PlayerMap<Pair<FoodLevelChangeEvent, (FoodLevelChangeEvent) -> Unit>> by lazy(::PlayerMap)
    public val bowTracker: MutableMap<LivingEntity, ItemStack> = mutableMapOf()

    @Suppress("kotlin:S3776")
    override suspend fun handleEnable() {
        event<BeaconEffectEvent>(EventPriority.HIGH, true) {
            if (shouldIgnore()) return@event

            player.getPotionEffect(effect.type)?.let { potion ->
                if (potion.duration > effect.duration && potion.fromOrigin()) return@event cancel()
            }
        }

        event<EntityShootBowEvent>(EventPriority.MONITOR, true) {
            val entity = this.entity as? LivingEntity ?: return@event
            val item = this.bow ?: return@event
            bowTracker[entity] = item
        }

        event<ProjectileHitEvent>(EventPriority.MONITOR) {
            val entity = this.entity.shooter as? LivingEntity ?: return@event
            val bow = bowTracker[entity] ?: return@event
            scheduler { bowTracker.remove(entity, bow) }.runAsyncTaskLater(plugin, 2.ticks)
        }

        event<PlayerJoinEvent>(EventPriority.LOWEST) { OriginHelper.activateOrigin(player, TerixPlayer[player].origin) }
        event<PlayerQuitEvent>(EventPriority.LOWEST) { OriginHelper.deactivateOrigin(player, TerixPlayer[player].origin) }
        event<PlayerKickEvent>(EventPriority.MONITOR, true) { OriginHelper.deactivateOrigin(player, TerixPlayer[player].origin) }

        event<PlayerPostRespawnEvent>(forceAsync = true) {
            OriginHelper.recalculateStates(player, TerixPlayer[player].origin)
            player.health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value
        }

        event<EntityPotionEffectEvent> { if (shouldCancel()) cancel() }

        event<EntityCombustEvent> {
            val player = entity as? Player ?: return@event
            if (ensureNoFire(player, TerixPlayer.cachedOrigin(player))) cancel()
        }

        event<EntityDamageEvent>(EventPriority.HIGH, true) {
            val player = entity as? Player ?: return@event
            val origin = TerixPlayer.cachedOrigin(player)

            if (ensureNoFire(player, origin) ||
                origin.damageActions[cause]?.invoke(this) != null &&
                damage == 0.0
            ) return@event cancel()
        }

        event<FoodLevelChangeEvent>(EventPriority.LOWEST) {
            val player = entity as? Player ?: return@event

            when {
                finishEventAction.contains(player) -> return@event
                item != null -> modifyFoodEvent(player, item!!, foodLevelChangeEvent = this)
                else -> { /* Do nothing */
                }
            }
        }

        event<FoodLevelChangeEvent>(EventPriority.MONITOR, false, forceAsync = true) {
            val player = this.entity as? Player ?: return@event
            val (event, action) = finishEventAction[player] ?: return@event

            if (this !== event) return@event // Not our event. Ignore.

            finishEventAction.remove(player)
            if (event.isCancelled) return@event

            action(this)

            val origin = TerixPlayer.cachedOrigin(player)

            origin.foodData.getAction(this.item!!)?.invoke(player)

            val nmsPlayer = player.handle
            val potions = origin.foodData.getProperties(this.item!!)?.effects?.filter { pair -> nmsPlayer.level.random.nextFloat() >= pair.second }

            if (!potions.isNullOrEmpty()) {
                sync {
                    potions.forEach { pair -> nmsPlayer.addEffect(pair.first, EntityPotionEffectEvent.Cause.FOOD) }
                }
            }

            player.sendHealthUpdate()
        }

        event<PlayerOriginChangeEvent>(
            ignoreCancelled = true,
            priority = EventPriority.LOWEST
        ) {
            StorageService.transaction {
                val databaseEntity = TerixPlayer[player].databaseEntity

                if (newOrigin === preOrigin) {
                    result = PlayerOriginChangeEvent.Result.CURRENT_ORIGIN
                    return@transaction cancel()
                }

                if (!skipRequirement && !databaseEntity.grants.contains(newOrigin.name) && newOrigin.requirements.isNotEmpty() && newOrigin.requirements.any { !it.second(player) }) {
                    result = PlayerOriginChangeEvent.Result.NO_PERMISSION
                    return@transaction cancel()
                }

                val now = now()
                if (!player.hasPermissionOrStar(TerixPermissions.selectionBypassCooldown.permission) && !bypassCooldown && databaseEntity.lastChosenTime + terixConfig.intervalBeforeChange > now) {
                    result = PlayerOriginChangeEvent.Result.ON_COOLDOWN
                    return@transaction cancel()
                }

                if (!player.hasPermissionOrStar(TerixPermissions.selectionBypassCooldown.permission) && !bypassCooldown) databaseEntity.lastChosenTime = now

                databaseEntity.origin = newOrigin
            }

            OriginHelper.changeTo(player, preOrigin, newOrigin) // TODO: This should cover the removeUnfulfilled method

            lang.origin.broadcast[
                "player" to { player.displayName() },
                "new_origin" to { newOrigin.displayName },
                "old_origin" to { preOrigin.displayName }
            ] message onlinePlayers

            if (terixConfig.showTitleOnChange) newOrigin.becomeOriginTitle?.invoke(player)
        }

        event(EventPriority.HIGHEST, ignoreCancelled = true, forceAsync = false, block = ::handle)

        event<PlayerGameModeChangeEvent> {
            if (this.newGameMode.ordinal in 1..2) {
                return@event OriginHelper.activateOrigin(player)
            }

            OriginHelper.deactivateOrigin(player)
        }

        this.soundHandlers()
        this.requirementHandlers()
    }

    private fun soundHandlers() {
        fun emitSound(
            player: Player,
            origin: Origin,
            function: KProperty1<SoundEffects, SoundEffect>
        ) {
            if (OriginHelper.shouldIgnorePlayer(player)) return

            val sound = function.get(origin.sounds)
            player.playSound(sound.resourceKey.asString(), sound.volume, sound.pitch, sound.distance)
        }

        eventFlow<PlayerDeathEvent>(priority = EventPriority.MONITOR, ignoreCancelled = true)
            .subscribe { event -> emitSound(event.entity, TerixPlayer.cachedOrigin(event.entity), SoundEffects::deathSound) }

        eventFlow<EntityDamageEvent>(priority = EventPriority.MONITOR, ignoreCancelled = true)
            .mapNotNull { event -> event.entity as? Player }
            .subscribe { player -> emitSound(player, TerixPlayer.cachedOrigin(player), SoundEffects::hurtSound) }
    }

    private fun requirementHandlers() {
        eventFlow<EntityPickupItemEvent>(priority = EventPriority.MONITOR, ignoreCancelled = true)
            .filter { event -> event.item.itemStack.type == Material.DRAGON_EGG }
            .mapNotNull { event -> event.entity as? Player }
            .filterNot { player -> player.pdc.has(DragonOrigin.CRADLE_KEY, PersistentDataType.BYTE) }
            .subscribe { player -> player.pdc.set(DragonOrigin.CRADLE_KEY, PersistentDataType.BYTE, 1) }

        eventFlow<PlayerDeathEvent>(priority = EventPriority.MONITOR, ignoreCancelled = true)
            .mapNotNull { event -> event.entity.killer }
            .mapNotNull { killer ->
                when (killer.world.environment) {
                    World.Environment.NORMAL -> DragonOrigin.KILL_OVERWORLD_KEY
                    World.Environment.NETHER -> DragonOrigin.KILL_NETHER_KEY
                    World.Environment.THE_END -> DragonOrigin.KILL_END_KEY
                    else -> return@mapNotNull null
                } to killer.pdc
            }.subscribe { (key, pdc) -> pdc.set(key, PersistentDataType.INTEGER, pdc.getOrDefault(key, PersistentDataType.INTEGER, 0) + 1) }
    }

    private val channels = hashMapOf<UUID, Channel<Boolean>>()

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun handle(event: PlayerSecondaryEvent) {
        if (alreadyEdible(event)) return

        val origin = TerixPlayer[event.player].origin
        val serverPlayer = event.player.handle
        val hand = serverPlayer.usedItemHand
        val itemStack = event.item!!
        val nmsItemStack = serverPlayer.getItemInHand(hand)

        val foodActions = origin.foodData.getAction(itemStack)
        val foodProperties = origin.foodData.getProperties(itemStack) ?: nmsItemStack.item.foodProperties // TODO -> Should we cancel the interaction if its just default?

        if (foodProperties == null && foodActions == null) return
        if (!serverPlayer.canEat(foodProperties?.canAlwaysEat() != false)) return // Always allow eating if there are only actions.

        channels.computeIfAbsent(event.player.uniqueId) {
            val channel = Channel<Boolean>(2)
            val finishEatingTime = now() + (if (foodProperties?.isFastFood == true) 16 else 32).ticks

            async {
                do {
                    delay(8.ticks)

                    val result = channel.tryReceive()
                    when {
                        result.isSuccess -> {
                            if (!result.getOrThrow()) {
                                logger.debug { "Channel received false for ${event.player.name}" }
                                break
                            }

                            logger.debug {
                                "Channel received true for ${event.player.name}, remaining time ${(finishEatingTime - now()).inWholeMilliseconds})"
                                event.player.playSound(Sound.ENTITY_GENERIC_EAT.key.key)
                            }

                            if (now() >= finishEatingTime) {
                                logger.debug { "PlayerRightClickEvent - Consumed all required." }
                                event.player.playSound(Sound.ENTITY_PLAYER_BURP.key.key)
                                modifyFoodEvent(event.player, itemStack, origin, serverPlayer, hand, nmsItemStack, foodProperties)
                                break
                            }
                        }
                        result.isClosed -> {
                            logger.debug { "Channel closed for ${event.player.name}" }
                            serverPlayer.stopUsingItem()
                            break
                        }
                        result.isFailure -> {
                            logger.debug { "Channel failed for ${event.player.name}" }
                            serverPlayer.stopUsingItem()
                            break
                        }
                    }
                } while (!channel.isEmpty && !channel.isClosedForReceive)

                logger.debug { "PlayerRightClickEvent - Channel closed for receive." }
                channels.computeAndRemove(event.player.uniqueId) { this.close() }
            }
            channel
        }.trySend(true).onSuccess {
            logger.debug { "PlayerRightClickEvent - Sent true to channel." }
        }.onFailure {
            logger.debug { "PlayerRightClickEvent - Failed to send true to channel." }
            channels.computeAndRemove(event.player.uniqueId) { this.close() }
        }
    }

    private fun alreadyEdible(event: PlayerSecondaryEvent): Boolean {
        if (!event.hasItem || event.item!!.type.isEdible) return true
        return false
    }

    // TODO -> This is a bit of a mess, clean it up.
    private fun modifyFoodEvent(
        player: Player,
        item: ItemStack,
        origin: Origin = TerixPlayer[player].origin,
        serverPlayer: NMSServerPlayer = player.handle,
        hand: InteractionHand = serverPlayer.usedItemHand,
        itemStack: NMSItemStack = serverPlayer.getItemInHand(hand),
        foodProperties: FoodProperties? = origin.foodData.getProperties(item),
        lambda: PlayerLambda? = origin.foodData.getAction(item),
        foodLevelChangeEvent: FoodLevelChangeEvent? = null
    ) {
        if (foodLevelChangeEvent?.isCancelled == true || (lambda == null && foodProperties == null)) {
            return logger.debug { "Nothing to do with this item." }
        }

        (foodLevelChangeEvent ?: FoodLevelChangeEvent(player, player.foodLevel, item)).apply {
            val nutrition = foodProperties?.nutrition ?: 0
            val saturation = if (foodProperties != null) nutrition * foodProperties.saturationModifier else 0f
            val oldFoodLevel = serverPlayer.foodData.foodLevel

            val newFoodLevel = oldFoodLevel + nutrition
            val newSaturationLevel = (serverPlayer.foodData.saturationLevel + saturation * 2.0f).coerceAtMost(newFoodLevel.toFloat())

            logger.debug { "New food level: $newFoodLevel" }
            logger.debug { "newSaturationLevel: $newSaturationLevel" }

            this.foodLevel = newFoodLevel

            val action = if (foodLevelChangeEvent == null) { event: FoodLevelChangeEvent ->
                item.amount--
                serverPlayer.foodData.eat(event.foodLevel - oldFoodLevel, foodProperties?.saturationModifier ?: 0f)

                sync { serverPlayer.awardStat(Stats.ITEM_USED[itemStack.item]) }
                CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, itemStack)
            } else { _ ->
                serverPlayer.foodData.foodLevel = newFoodLevel
                serverPlayer.foodData.saturationLevel = newSaturationLevel
            }

            logger.debug { foodProperties?.effects?.joinToString(", ") { it.first.toString() } }

            finishEventAction[player] = this to action
            if (foodLevelChangeEvent == null) sync { callEvent() }
        }
    }

    private fun BeaconEffectEvent.shouldIgnore(): Boolean {
        val potion = player.getPotionEffect(effect.type) ?: return true
        return potion.duration < effect.duration || !potion.hasKey() || !potion.fromOrigin()
    }

    private fun ensureNoFire(
        player: Player,
        origin: Origin
    ): Boolean {
        if (player.fireTicks <= 0 || !origin.fireImmunity) return false
        player.fireTicks = 0
        return true
    }

    private fun EntityPotionEffectEvent.shouldCancel() = entity is Player &&
        cause == EntityPotionEffectEvent.Cause.MILK &&
        oldEffect != null && oldEffect!!.hasKey() &&
        oldEffect!!.fromOrigin()

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> Flow<T>.subscribe(action: suspend (T) -> Unit) {
        this.onEach(action).launchIn(CoroutineScope(supervisor.newCoroutineContext(dispatcher.get())))
    }
}
