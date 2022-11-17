package dev.racci.terix.core.services

import arrow.core.Either
import com.destroystokyo.paper.event.block.BeaconEffectEvent
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.collections.PlayerMap
import dev.racci.minix.api.events.keybind.PlayerSecondaryEvent
import dev.racci.minix.api.events.player.PlayerLiquidEnterEvent
import dev.racci.minix.api.events.player.PlayerLiquidExitEvent
import dev.racci.minix.api.events.player.PlayerMoveFullXYZEvent
import dev.racci.minix.api.events.world.WorldDayEvent
import dev.racci.minix.api.events.world.WorldNightEvent
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.collections.computeAndRemove
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.events
import dev.racci.minix.api.extensions.hasPermissionOrStar
import dev.racci.minix.api.extensions.inOverworld
import dev.racci.minix.api.extensions.onlinePlayers
import dev.racci.minix.api.extensions.pdc
import dev.racci.minix.api.extensions.scheduler
import dev.racci.minix.api.flow.eventFlow
import dev.racci.minix.api.services.DataService
import dev.racci.minix.api.services.DataService.Companion.inject
import dev.racci.minix.api.utils.now
import dev.racci.minix.api.utils.ticks
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.Terix
import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.data.OriginNamespacedTag
import dev.racci.terix.api.data.TerixConfig
import dev.racci.terix.api.dsl.TimedAttributeBuilder
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.extensions.playSound
import dev.racci.terix.api.origins.OriginHelper
import dev.racci.terix.api.origins.OriginHelper.activateOrigin
import dev.racci.terix.api.origins.OriginHelper.deactivateOrigin
import dev.racci.terix.api.origins.origin.ActionPropBuilder
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.sounds.SoundEffect
import dev.racci.terix.api.origins.sounds.SoundEffects
import dev.racci.terix.api.origins.states.State
import dev.racci.terix.api.origins.states.State.Companion.convertLiquidToState
import dev.racci.terix.core.commands.TerixPermissions
import dev.racci.terix.core.data.Lang
import dev.racci.terix.core.extensions.fromOrigin
import dev.racci.terix.core.extensions.message
import dev.racci.terix.core.extensions.originTime
import dev.racci.terix.core.extensions.sanitise
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
import net.minecraft.server.level.ServerPlayer
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
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerGameModeChangeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.inject
import java.util.UUID
import kotlin.reflect.KProperty1
import kotlin.time.Duration.Companion.seconds

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

        event<PlayerQuitEvent>(EventPriority.MONITOR, true) { player.sanitise() }

        event<PlayerJoinEvent>(forceAsync = true) {
            val origin = TerixPlayer.cachedOrigin(player)
            removeUnfulfilledOrInvalidAttributes(player, origin) // Sometimes we can miss some attributes, so we need to remove them
            activateOrigin(player, origin)

            delay(0.250.seconds)
            player.sendHealthUpdate()
        }

        event<PlayerPostRespawnEvent>(forceAsync = true) {
            val origin = TerixPlayer.cachedOrigin(player)
            removeUnfulfilledOrInvalidAttributes(player, origin)
            activateOrigin(player, origin)

            player.health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value
        }

        event<EntityPotionEffectEvent> { if (shouldCancel()) cancel() }

        event<WorldNightEvent>(forceAsync = true) { switchTimeStates(State.TimeState.NIGHT) }
        event<WorldDayEvent>(forceAsync = true) { switchTimeStates(State.TimeState.DAY) }

        event<PlayerChangedWorldEvent>(forceAsync = true) {
            val lastState = State.getEnvironmentState(from.environment)
            val newState = State.getEnvironmentState(player.world.environment)
            val origin = TerixPlayer.cachedOrigin(player)

            if (lastState == newState) return@event

            State.getTimeState(from)?.deactivate(player, origin)
            lastState.exchange(player, origin, newState)
        }

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

            origin.customFoodActions[this.item!!.type]?.forEach { it.fold({ it(player) }, { it(player) }) }

            val nmsPlayer = player.toNMS()
            val potions = origin.customFoodProperties[this.item!!.type]?.effects?.filter { pair -> nmsPlayer.level.random.nextFloat() >= pair.second }

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
            if (newOrigin === preOrigin) {
                result = PlayerOriginChangeEvent.Result.CURRENT_ORIGIN
                return@event cancel()
            }

            if (!skipRequirement && !StorageService.transaction { TerixPlayer[player].grants.contains(newOrigin.name) } && newOrigin.requirements.isNotEmpty() && newOrigin.requirements.any { !it.second(player) }) {
                result = PlayerOriginChangeEvent.Result.NO_PERMISSION
                return@event cancel()
            }

            val now = now()
            if (!this.player.hasPermissionOrStar(TerixPermissions.selectionBypassCooldown.permission) && !bypassCooldown && player.originTime + terixConfig.intervalBeforeChange > now) {
                result = PlayerOriginChangeEvent.Result.ON_COOLDOWN
                return@event cancel()
            }

            if (!this.player.hasPermissionOrStar(TerixPermissions.selectionBypassCooldown.permission) && !bypassCooldown) player.originTime = now

            transaction(getKoin().getProperty("terix:database")) { TerixPlayer[player.uniqueId].origin = newOrigin }
            OriginHelper.changeTo(player, preOrigin, newOrigin) // TODO: This should cover the removeUnfulfilled method
            removeUnfulfilledOrInvalidAttributes(player, newOrigin)

            lang.origin.broadcast[
                "player" to { player.displayName() },
                "new_origin" to { newOrigin.displayName },
                "old_origin" to { preOrigin.displayName }
            ] message onlinePlayers

            if (terixConfig.showTitleOnChange) newOrigin.becomeOriginTitle?.invoke(player)

            preOrigin.handleChangeOrigin(this)
            newOrigin.handleBecomeOrigin(this)
        }

        event(EventPriority.HIGHEST, ignoreCancelled = true, forceAsync = false, block = ::handle)

        event<PlayerMoveFullXYZEvent> {
            val lastState = State.getBiomeState(from)
            val currentState = State.getBiomeState(to)
            if (lastState == currentState) return@event

            val origin = TerixPlayer.cachedOrigin(player)

            lastState?.deactivate(player, origin)
            currentState?.activate(player, origin)
        }

        event<PlayerGameModeChangeEvent> {
            if (this.newGameMode.ordinal in 1..2) {
                return@event activateOrigin(player)
            }

            deactivateOrigin(player)
        }

        events(
            PlayerJoinEvent::class,
            PlayerPostRespawnEvent::class,
            PlayerOriginChangeEvent::class,
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
        ) { TerixPlayer.cachedOrigin(player).handleLoad(player) }

        this.stateHandlers()
        this.soundHandlers()
        this.requirementHandlers()
    }

    private fun stateHandlers() {
        event<PlayerLiquidEnterEvent> { convertLiquidToState(previousType).exchange(player, TerixPlayer.cachedOrigin(player), convertLiquidToState(newType)) }
        event<PlayerLiquidExitEvent> { convertLiquidToState(previousType).exchange(player, TerixPlayer.cachedOrigin(player), convertLiquidToState(newType)) }
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

        val origin = TerixPlayer.cachedOrigin(event.player)
        val serverPlayer = event.player.toNMS()
        val hand = serverPlayer.usedItemHand
        val itemStack = serverPlayer.getItemInHand(hand)

        val foodInfo = origin.customMatcherFoodProperties.entries.find { it.key.matches(event.item!!) }?.value ?: origin.customFoodProperties[event.item!!.type] ?: itemStack.item.foodProperties
        val foodActions = origin.customFoodActions[event.item!!.type]

        if (foodInfo == null && foodActions == null) return
        if (!serverPlayer.canEat(foodInfo?.canAlwaysEat() != false)) return // Always allow eating if there are only actions.

        channels.computeIfAbsent(event.player.uniqueId) {
            val channel = Channel<Boolean>(2)
            val finishEatingTime = now() + (if (foodInfo?.isFastFood == true) 16 else 32).ticks

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
                                modifyFoodEvent(event.player, event.item!!, origin, serverPlayer, hand, itemStack, foodInfo)
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

    private fun modifyFoodEvent(
        player: Player,
        item: ItemStack,
        origin: Origin = TerixPlayer.cachedOrigin(player),
        serverPlayer: ServerPlayer = player.toNMS(),
        hand: InteractionHand = serverPlayer.usedItemHand,
        itemStack: net.minecraft.world.item.ItemStack = serverPlayer.getItemInHand(hand),
        foodInfo: FoodProperties? = origin.customFoodProperties[item.type],
        actions: Collection<Either<ActionPropBuilder, TimedAttributeBuilder>>? = origin.customFoodActions[item.type],
        foodLevelChangeEvent: FoodLevelChangeEvent? = null
    ) {
        if (foodLevelChangeEvent?.isCancelled == true || (actions.isNullOrEmpty() && foodInfo == null)) {
            return logger.debug { "Nothing to do with this item." }
        } // There's nothing to do here

        (foodLevelChangeEvent ?: FoodLevelChangeEvent(player, player.foodLevel, item)).apply {
            val nutrition = foodInfo?.nutrition ?: 0
            val saturation = if (foodInfo != null) nutrition * foodInfo.saturationModifier else 0f
            val oldFoodLevel = serverPlayer.foodData.foodLevel

            val newFoodLevel = oldFoodLevel + nutrition
            val newSaturationLevel = (serverPlayer.foodData.saturationLevel + saturation * 2.0f).coerceAtMost(newFoodLevel.toFloat())

            logger.debug { "New food level: $newFoodLevel" }
            logger.debug { "newSaturationLevel: $newSaturationLevel" }

            this.foodLevel = newFoodLevel

            val action = if (foodLevelChangeEvent == null) { event: FoodLevelChangeEvent ->
                item.amount--
                serverPlayer.foodData.eat(event.foodLevel - oldFoodLevel, foodInfo?.saturationModifier ?: 0f)

                sync { serverPlayer.awardStat(Stats.ITEM_USED[itemStack.item]) }
                CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, itemStack)
            } else { _ ->
                serverPlayer.foodData.foodLevel = newFoodLevel
                serverPlayer.foodData.saturationLevel = newSaturationLevel
            }

            logger.debug { foodInfo?.effects?.joinToString(", ") { it.first.toString() } }

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

    private fun removeUnfulfilledOrInvalidAttributes(
        player: Player,
        origin: Origin,
        activeTriggers: Set<State> = State.getPlayerStates(player)
    ) {
        for (attribute in Attribute.values()) {
            val inst = player.getAttribute(attribute) ?: continue

            for (modifier in inst.modifiers) {
                val tag = OriginNamespacedTag.fromString(modifier.name) ?: continue
                val state = tag.getState() ?: continue

                if (state !in activeTriggers || !tag.fromOrigin(origin)) {
                    logger.debug { "Removing unfulfilled or invalid attribute: $modifier" }
                    inst.removeModifier(modifier)
                    continue
                }

                // Make sure the attribute is unchanged
                origin.attributeModifiers[state]?.firstOrNull {
                    it.first == attribute &&
                        it.second.name == modifier.name &&
                        it.second.amount == modifier.amount &&
                        it.second.operation == modifier.operation &&
                        it.second.slot == modifier.slot
                } ?: inst.removeModifier(modifier)
            }
        }
    }

    private suspend fun switchTimeStates(state: State) {
        onlinePlayers.filter(Player::inOverworld)
            .onEach { player ->
                val origin = TerixPlayer.cachedOrigin(player)
                val oldState = if (state === State.TimeState.DAY) State.TimeState.NIGHT else State.TimeState.DAY
                oldState.exchange(player, origin, state)
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> Flow<T>.subscribe(action: suspend (T) -> Unit) {
        this.onEach(action).launchIn(CoroutineScope(supervisor.newCoroutineContext(dispatcher.get())))
    }

    public companion object : ExtensionCompanion<ListenerService>()
}
