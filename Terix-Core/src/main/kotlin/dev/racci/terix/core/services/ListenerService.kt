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
import dev.racci.minix.api.extensions.inOverworld
import dev.racci.minix.api.extensions.onlinePlayers
import dev.racci.minix.api.extensions.reflection.safeCast
import dev.racci.minix.api.extensions.scheduler
import dev.racci.minix.api.services.DataService
import dev.racci.minix.api.services.DataService.Companion.inject
import dev.racci.minix.api.utils.now
import dev.racci.minix.api.utils.ticks
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.Terix
import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.data.TerixConfig
import dev.racci.terix.api.dsl.AttributeModifierBuilder
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.dsl.TimedAttributeBuilder
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.extensions.playSound
import dev.racci.terix.api.origins.OriginHelper
import dev.racci.terix.api.origins.OriginHelper.activateOrigin
import dev.racci.terix.api.origins.OriginHelper.deactivateOrigin
import dev.racci.terix.api.origins.enums.KeyBinding
import dev.racci.terix.api.origins.origin.ActionPropBuilder
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.sounds.SoundEffect
import dev.racci.terix.api.origins.sounds.SoundEffects
import dev.racci.terix.api.origins.states.State
import dev.racci.terix.api.origins.states.State.Companion.convertLiquidToState
import dev.racci.terix.core.data.Lang
import dev.racci.terix.core.extensions.message
import dev.racci.terix.core.extensions.originTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.delay
import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.food.FoodProperties
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerGameModeChangeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.inject
import java.util.UUID
import kotlin.reflect.KProperty1
import kotlin.time.Duration.Companion.seconds

// Check for button presses to invoke actions in the test chambers
// TODO -> Light entities on fire when hit with a torch
@MappedExtension(Terix::class, "Listener Service", [DataService::class])
class ListenerService(override val plugin: Terix) : Extension<Terix>() {
    private val terixConfig by inject<DataService>().inject<TerixConfig>()
    private val lang by inject<DataService>().inject<Lang>()
    private val finishEventAction: PlayerMap<Pair<FoodLevelChangeEvent, (FoodLevelChangeEvent) -> Unit>> by lazy(::PlayerMap)
    val bowTracker = mutableMapOf<LivingEntity, ItemStack>()

    @Suppress("kotlin:S3776")
    override suspend fun handleEnable() {
        event<BeaconEffectEvent>(priority = EventPriority.LOWEST) {
            if (shouldIgnore()) return@event

            player.getPotionEffect(effect.type)?.let { potion ->
                if (potion.duration > effect.duration &&
                    potion.key?.asString()?.matches(PotionEffectBuilder.regex) == true
                ) return@event cancel()
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

        fun getInvalidPotions(
            player: Player,
            origin: Origin,
            states: Set<State>
        ): List<PotionEffectType> {
            val activeEffects = player.activePotionEffects
            val invalidTypes = arrayOfNulls<PotionEffectType>(activeEffects.size)

            for ((index, potion) in player.activePotionEffects.withIndex()) {
                val state = OriginHelper.potionState(potion)
                val potOrigin = OriginHelper.potionOrigin(potion)
                if ((state == null || state in states) && potOrigin == null || potOrigin === origin) continue

                invalidTypes[index] = potion.type
            }

            return invalidTypes.filterNotNull()
        }

        event<PlayerJoinEvent>(forceAsync = true) {
            val origin = TerixPlayer.cachedOrigin(player)
            State.recalculateAllStates(player)
            val states = State.getPlayerStates(player)
            val invalidPotions = getInvalidPotions(player, origin, states)

            logger.debug { "Found ${invalidPotions.size} invalid potions for ${player.name}" }

            sync { invalidPotions.forEach(player::removePotionEffect) }
            removeUnfulfilledOrInvalidAttributes(player, states)

            delay(0.5.seconds)
            player.sendHealthUpdate()
        }

        event<PlayerPostRespawnEvent>(forceAsync = true) {
            removeUnfulfilledOrInvalidAttributes(player)

            val origin = TerixPlayer.cachedOrigin(player)
            val state = State.getEnvironmentState(player.world.environment)

            OriginHelper.applyBase(player, origin)
            state.activate(player, origin)

            player.health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value
        }

        event<EntityPotionEffectEvent> { if (shouldCancel()) cancel() }

        event<PlayerLiquidEnterEvent> { convertLiquidToState(previousType).exchange(player, TerixPlayer.cachedOrigin(player), convertLiquidToState(newType)) }
        event<PlayerLiquidExitEvent> { convertLiquidToState(previousType).exchange(player, TerixPlayer.cachedOrigin(player), convertLiquidToState(newType)) }

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

        event<EntityDamageEvent>(
            ignoreCancelled = true,
            priority = EventPriority.HIGHEST
        ) {
            val player = entity as? Player ?: return@event
            val origin = TerixPlayer.cachedOrigin(player)

            if (ensureNoFire(player, origin) ||
                origin.damageActions[cause]?.invoke(this) != null &&
                damage == 0.0
            ) return@event cancel()

            originSound(player, origin, SoundEffects::hurtSound)
        }

        event<PlayerDeathEvent>(
            ignoreCancelled = true,
            priority = EventPriority.HIGHEST
        ) { originSound(player, TerixPlayer.cachedOrigin(player), SoundEffects::hurtSound) }

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
            val player = this.entity.safeCast<Player>() ?: run {
                logger.debug { "FoodLevelChangeEvent was not a player" }
                return@event
            }
            val (event, action) = finishEventAction[player] ?: run {
                logger.debug { "finishEventAction didn't contain event." }
                return@event
            }

            if (this !== event) {
                logger.debug { "FoodLevelChangeEvent was not the same as the original event." }
                return@event
            } // Not our event. Ignore.
            logger.debug { "Finishing food level change event for ${player.name}" }

            finishEventAction.remove(player)
            if (event.isCancelled) return@event

            action(this)

            val origin = TerixPlayer.cachedOrigin(player)

            origin.customFoodActions[this.item!!.type]?.forEach { it.fold({ it(player) }, { it(player) }) }
            origin.customFoodProperties[this.item!!.type]?.effects?.forEach { pair ->
                if (player.world.toNMS().random.nextFloat() >= pair.second) return@forEach
                player.toNMS().addEffect(MobEffectInstance(pair.first), EntityPotionEffectEvent.Cause.FOOD)
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

            if (!(skipRequirement || newOrigin.hasPermission(player))) {
                result = PlayerOriginChangeEvent.Result.NO_PERMISSION
                return@event cancel()
            }

            val now = now()
            if (!(bypassCooldown || player.originTime + terixConfig.intervalBeforeChange > now)) {
                result = PlayerOriginChangeEvent.Result.ON_COOLDOWN
                return@event cancel()
            }

            if (!bypassCooldown) player.originTime = now

            transaction(getKoin().getProperty("terix:database")) { TerixPlayer[player.uniqueId].origin = newOrigin }
            OriginHelper.changeTo(player, preOrigin, newOrigin) // TODO: This should cover the removeUnfulfilled method
            removeUnfulfilledOrInvalidAttributes(player)

            lang.origin.broadcast[
                "player" to { player.displayName() },
                "new_origin" to { newOrigin.displayName },
                "old_origin" to { preOrigin.displayName }
            ] message onlinePlayers

            if (terixConfig.showTitleOnChange) newOrigin.becomeOriginTitle?.invoke(player)

            preOrigin.handleChangeOrigin(this)
            newOrigin.handleBecomeOrigin(this)
        }

        events(*KeyBinding.values().map(KeyBinding::event).toTypedArray()) {
            val clazz = KeyBinding.fromEvent(this::class)
            val origin = TerixPlayer.cachedOrigin(player)

            val ability = origin.abilities[clazz] ?: return@events
            ability.toggle(player)
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
    }

    private val channels = hashMapOf<UUID, Channel<Boolean>>()

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun handle(event: PlayerSecondaryEvent) {
        if (alreadyEdible(event)) return

        val origin = TerixPlayer.cachedOrigin(event.player)
        val serverPlayer = event.player.toNMS()
        val hand = serverPlayer.usedItemHand
        val itemStack = serverPlayer.getItemInHand(hand)

        val foodInfo = origin.customFoodProperties[event.item!!.type] ?: itemStack.item.foodProperties
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
        return potion.duration < effect.duration ||
            !potion.hasKey() ||
            !potion.key!!.asString().matches(PotionEffectBuilder.regex)
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
        oldEffect!!.key!!.asString().matches(PotionEffectBuilder.regex)

    private fun removeUnfulfilledOrInvalidAttributes(
        player: Player,
        activeTriggers: Set<State> = State.getPlayerStates(player)
    ) {
        for (attribute in Attribute.values()) {
            val inst = player.getAttribute(attribute) ?: continue
            val origin = TerixPlayer.cachedOrigin(player)

            for (modifier in inst.modifiers) {
                val match = AttributeModifierBuilder.regex.find(modifier.name)?.groups ?: continue
                val state = State.valueOf(match["state"]!!.value.uppercase())

                if (state !in activeTriggers ||
                    match["origin"]!!.value != origin.name.lowercase()
                ) {
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

    private fun originSound(
        player: Player,
        origin: Origin,
        function: KProperty1<SoundEffects, SoundEffect>
    ) {
        if (OriginHelper.shouldIgnorePlayer(player)) return

        val sound = function.get(origin.sounds)
        player.playSound(sound.resourceKey.asString(), sound.volume, sound.pitch, sound.distance)
    }

    companion object : ExtensionCompanion<ListenerService>()
}
