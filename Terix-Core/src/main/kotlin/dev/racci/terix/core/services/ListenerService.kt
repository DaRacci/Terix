package dev.racci.terix.core.services

import com.destroystokyo.paper.event.block.BeaconEffectEvent
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.events.PlayerEnterLiquidEvent
import dev.racci.minix.api.events.PlayerExitLiquidEvent
import dev.racci.minix.api.events.PlayerMoveFullXYZEvent
import dev.racci.minix.api.events.PlayerRightClickEvent
import dev.racci.minix.api.events.WorldDayEvent
import dev.racci.minix.api.events.WorldNightEvent
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.events
import dev.racci.minix.api.extensions.inOverworld
import dev.racci.minix.api.extensions.log
import dev.racci.minix.api.extensions.onlinePlayers
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
import kotlinx.coroutines.delay
import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.food.FoodProperties
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.craftbukkit.v1_19_R1.event.CraftEventFactory
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
import org.bukkit.potion.PotionEffect
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
    private val customEvent = mutableListOf<Player>()
    val bowTracker = mutableMapOf<LivingEntity, ItemStack>()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Suppress("kotlin:S3776")
    override suspend fun handleEnable() {
        event<BeaconEffectEvent>(priority = EventPriority.LOWEST) {
            if (shouldIgnore()) return@event

            player.getPotionEffect(effect.type)?.let { potion ->
                if (potion.duration > effect.duration &&
                    potion.key?.asString()?.matches(PotionEffectBuilder.regex) == true
                ) return@event cancel()
            }

            // TODO: Move into the Elixir Plugin
            effect = PotionEffect(
                effect.type,
                effect.duration,
                effect.amplifier,
                true,
                false,
                false
            )
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

            log.debug { "Found ${invalidPotions.size} invalid potions for ${player.name}" }

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

        event<PlayerEnterLiquidEvent> { convertLiquidToState(previousType).exchange(player, TerixPlayer.cachedOrigin(player), convertLiquidToState(newType)) }
        event<PlayerExitLiquidEvent> { convertLiquidToState(previousType).exchange(player, TerixPlayer.cachedOrigin(player), convertLiquidToState(newType)) }

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
            priority = EventPriority.LOWEST
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
            priority = EventPriority.LOWEST
        ) { originSound(player, TerixPlayer.cachedOrigin(player), SoundEffects::hurtSound) }

        event<FoodLevelChangeEvent>(
            ignoreCancelled = true,
            priority = EventPriority.LOWEST
        ) {
            val player = entity as? Player ?: return@event

            when {
                customEvent.contains(player) -> customEvent.remove(player)
                item != null -> foodEvent(player, item!!, foodLevelChangeEvent = this)
                else -> { /* Do nothing */
                }
            }
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
        }

        events(*KeyBinding.values().map(KeyBinding::event).toTypedArray()) {
            val clazz = KeyBinding.fromEvent(this::class)
            val origin = TerixPlayer.cachedOrigin(player)

            val ability = origin.abilities[clazz] ?: return@events
            ability.toggle(player)
        }

        val channels = hashMapOf<UUID, Channel<Boolean>>()
        val lasts = hashMapOf<UUID, Long>()
        event<PlayerRightClickEvent> {
            if (!this.hasItem || this.item!!.type.isEdible) return@event

            var now = now().toEpochMilliseconds()
            val origin = TerixPlayer.cachedOrigin(player)
            val serverPlayer = player.toNMS()
            val hand = serverPlayer.usedItemHand
            val itemStack = serverPlayer.getItemInHand(hand)
            val foodInfo = origin.customFoodProperties[item!!.type] ?: itemStack.item.foodProperties
            if (serverPlayer.canEat(itemStack.item.foodProperties?.canAlwaysEat() == true)) return@event

            val channel = channels.computeIfAbsent(player.uniqueId) {
                val channel = Channel<Boolean>(2) // 2 So we can overflow in case of lag
                lasts[player.uniqueId] = now

                log.debug { "PlayerRightClickEvent - channel created with empty state ${channel.isEmpty}" }

                async {
                    var required = 1600L
                    do {
                        delay(400)

                        now = now().toEpochMilliseconds()
                        val received = channel.tryReceive()

                        when {
                            received.isSuccess -> {
                                log.debug { "PlayerRightClickEvent - received ${received.getOrNull()}" }
                                val elapsed = now - (lasts.replace(player.uniqueId, now) ?: 0L)
                                required -= elapsed
                                player.playSound(Sound.ENTITY_GENERIC_EAT.key.key)
                            }
                            received.isFailure -> {
                                log.debug { "Missed receiving." }
                                continue
                            }
                            received.isClosed -> break
                        }

                        if (channel.isEmpty) {
                            log.debug { "PlayerRightClickEvent - Channel is empty" }
                            continue
                        }

                        if (required == 0L) {
                            log.debug { "PlayerRightClickEvent - Consumed all required." }
                            player.playSound(Sound.ENTITY_PLAYER_BURP.key.key)
                            foodEvent(player, this@event.item!!, origin, serverPlayer, hand, itemStack, foodInfo)
                            break
                        }
                    } while (!channel.isEmpty && !channel.isClosedForReceive)

                    log.debug { "PlayerRightClickEvent - Channel closed for receive." }
                    channel.close()
                    channels.remove(player.uniqueId, channel)
                }
                channel
            }

            if (channel.trySend(true).isSuccess) {
                log.debug { "PlayerRightClickEvent - Sent element" }
            } else {
                log.debug { "PlayerRightClickEvent - Channel is full" }
                channel.close()
                channels.remove(player.uniqueId, channel)
            }
        }

        event<PlayerMoveFullXYZEvent>() {
            val lastState = State.getBiomeState(from)
            val currentState = State.getBiomeState(to)
            if (lastState == currentState) return@event

            val origin = TerixPlayer.cachedOrigin(player)

            lastState?.deactivate(player, origin)
            currentState?.activate(player, origin)
        }

        event<PlayerGameModeChangeEvent>() {
            if (this.newGameMode.ordinal in 1..2) {
                return@event activateOrigin(player)
            }

            deactivateOrigin(player)
        }
    }

    private suspend fun foodEvent(
        player: Player,
        item: ItemStack,
        origin: Origin = TerixPlayer.cachedOrigin(player),
        serverPlayer: ServerPlayer = player.toNMS(),
        hand: InteractionHand = serverPlayer.usedItemHand,
        itemStack: net.minecraft.world.item.ItemStack = serverPlayer.getItemInHand(hand),
        foodInfo: FoodProperties? = origin.customFoodProperties[item.type],
        attributes: Collection<TimedAttributeBuilder>? = origin.foodAttributes[item.type],
        action: ActionPropBuilder? = origin.foodBlocks[item.type],
        foodLevelChangeEvent: FoodLevelChangeEvent? = null
    ) {
        if (foodLevelChangeEvent?.isCancelled == true || (action == null && attributes == null && foodInfo == null)) {
            return log.debug { "Nothing to do with this item." }
        } // There's nothing to do here

        if (foodInfo != null) {
            val foodData = serverPlayer.foodData
            val oldFoodLevel = serverPlayer.foodData.foodLevel

            if (foodLevelChangeEvent != null) {
                val newFoodLevel = foodData.foodLevel + foodInfo.nutrition
                val newSaturationLevel = (foodData.saturationLevel + foodInfo.nutrition * foodInfo.saturationModifier * 2.0f).coerceAtMost(foodData.foodLevel.toFloat())

                log.debug { "Modifying existing foodLevelChangeEvent" }
                log.debug { "Old food level: $oldFoodLevel" }
                log.debug { "New food level: $newFoodLevel" }
                log.debug { "newSaturationLevel: $newSaturationLevel" }

                foodLevelChangeEvent.foodLevel = newFoodLevel
                scheduler {
                    if (foodLevelChangeEvent.isCancelled) return@scheduler
                    serverPlayer.foodData.setSaturation(newSaturationLevel)
                }.runAsyncTaskLater(plugin, 1.ticks)
                return halal(player, attributes, action)
            }

            customEvent += player
            val event = CraftEventFactory.callFoodLevelChangeEvent(serverPlayer, foodInfo.nutrition + oldFoodLevel, itemStack)
            log.debug { "Calling new FoodLevelChangeEvent" }

            if (!event.isCancelled) {
                log.debug { "FoodLevelChangeEvent is not cancelled" }
                log.debug { "Old food level: $oldFoodLevel" }
                log.debug { "New food level: ${event.foodLevel}" }

                serverPlayer.foodData.eat(serverPlayer.foodData.foodLevel - oldFoodLevel, foodInfo.saturationModifier)
                serverPlayer.awardStat(Stats.ITEM_USED[itemStack.item])
                CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, itemStack)

                player.sendHealthUpdate()
                return halal(player, attributes, action)
            } else customEvent -= player // Ensure that we don't have a ghost event.
        }
    }

    private suspend fun halal(
        player: Player,
        attributes: Collection<TimedAttributeBuilder>?,
        action: ActionPropBuilder?
    ) {
        attributes?.forEach { it.invoke(player) }
        action?.invoke(player)
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
                    log.debug { "Removing unfulfilled or invalid attribute: $modifier" }
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
