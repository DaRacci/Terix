package dev.racci.terix.core.services

import com.destroystokyo.paper.event.block.BeaconEffectEvent
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.events.LiquidType
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
import dev.racci.minix.api.events.WorldDayEvent
import dev.racci.minix.api.events.WorldNightEvent
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.async
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.events
import dev.racci.minix.api.extensions.inOverworld
import dev.racci.minix.api.extensions.onlinePlayers
import dev.racci.minix.api.services.DataService
import dev.racci.minix.api.services.DataService.Companion.inject
import dev.racci.minix.api.utils.now
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.Terix
import dev.racci.terix.api.dsl.AttributeModifierBuilder
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.ensureMainThread
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.extensions.playSound
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.KeyBinding
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.api.origins.enums.Trigger.Companion.getTimeTrigger
import dev.racci.terix.api.origins.enums.Trigger.Companion.getTrigger
import dev.racci.terix.core.data.Config
import dev.racci.terix.core.data.Lang
import dev.racci.terix.core.data.PlayerData
import dev.racci.terix.core.extensions.activeTriggers
import dev.racci.terix.core.extensions.message
import dev.racci.terix.core.extensions.nightVision
import dev.racci.terix.core.extensions.origin
import dev.racci.terix.core.extensions.originTime
import dev.racci.terix.core.origins.invokeAdd
import dev.racci.terix.core.origins.invokeBase
import dev.racci.terix.core.origins.invokeReload
import dev.racci.terix.core.origins.invokeRemove
import dev.racci.terix.core.origins.invokeSwap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import org.bukkit.attribute.Attribute
import org.bukkit.craftbukkit.v1_19_R1.event.CraftEventFactory
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.koin.core.component.inject
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// Check for button presses to invoke actions in the test chambers
@MappedExtension(Terix::class, "Listener Service", [DataService::class])
class ListenerService(override val plugin: Terix) : Extension<Terix>() {
    private val config by inject<DataService>().inject<Config>()
    private val lang by inject<DataService>().inject<Lang>()

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

        event<PlayerJoinEvent>(forceAsync = true) {

            val origin = player.origin()
            val activeTriggers = player.activeTriggers()
            val activeTriggersStr = activeTriggers.map { it.name.lowercase() }

            val potions = mutableListOf<PotionEffectType>()
            if (origin.nightVision && player.nightVision.name.lowercase() !in activeTriggersStr) potions.add(PotionEffectType.NIGHT_VISION)
            for (potion in player.activePotionEffects) {
                val key = potion.key?.asString() ?: continue
                val match = PotionEffectBuilder.regex.find(key)?.groups?.get("trigger") ?: continue
                if (match.value in activeTriggersStr) continue

                potions += potion.type
            }
            ensureMainThread { potions.forEach(player::removePotionEffect) }
            removeUnfulfilledOrInvalidAttributes(player, activeTriggers)

            // Trigger.invokeBase(player) // I don't think this is needed anymore
            // player.world.environment.getTrigger().invokeAdd(player) // Also may not be needed
            delay(0.5.seconds)
            player.sendHealthUpdate()
        }

        event<PlayerPostRespawnEvent>(forceAsync = true) {
            removeUnfulfilledOrInvalidAttributes(player)
            Trigger.invokeBase(player)
            player.world.environment.getTrigger().invokeAdd(player)
            player.health = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value
        }

        event<EntityPotionEffectEvent> { if (shouldCancel()) cancel() }

        event<PlayerEnterLiquidEvent> {
            Trigger.values().find { it.name == newType.name }?.invokeAdd(player)
            if (previousType != LiquidType.NON) return@event
            Trigger.LAND.invokeRemove(player)
        }
        event<PlayerExitLiquidEvent> {
            Trigger.values().find { it.name == previousType.name }?.invokeRemove(player)
            if (newType != LiquidType.NON) return@event
            Trigger.LAND.invokeAdd(player)
        }

        event<WorldNightEvent>(forceAsync = true) { timeTrigger(Trigger.NIGHT) }
        event<WorldDayEvent>(forceAsync = true) { timeTrigger(Trigger.DAY) }

        event<PlayerChangedWorldEvent>(forceAsync = true) {
            val fromTrigger = from.environment.getTrigger()
            val toTrigger = player.world.environment.getTrigger()

            if (fromTrigger == toTrigger) return@event
            if (fromTrigger.ordinal == 4 && toTrigger.ordinal != 4) from.getTimeTrigger()!!.invokeRemove(player)

            toTrigger.invokeSwap(fromTrigger, player)
            player.origin().titles[toTrigger]?.invoke(player)
        }

        event<EntityCombustEvent> {
            val player = entity as? Player ?: return@event
            if (ensureNoFire(player, player.origin())) cancel()
        }

        event<EntityDamageEvent>(
            ignoreCancelled = true,
            priority = EventPriority.LOWEST
        ) {
            val player = entity as? Player ?: return@event
            val origin = player.origin()

            if (ensureNoFire(player, origin) ||
                origin.damageActions[cause]?.invoke(this) != null &&
                damage == 0.0
            ) return@event cancel()

            val sound = player.origin().sounds.hurtSound
            player.playSound(sound.resourceKey.asString(), sound.volume, sound.pitch, sound.distance)
        }

        event<PlayerDeathEvent>(
            ignoreCancelled = true,
            priority = EventPriority.LOWEST
        ) {
            val sound = player.origin().sounds.deathSound
            player.playSound(sound.resourceKey.asString(), sound.volume, sound.pitch, sound.distance)
        }

        event<FoodLevelChangeEvent>(
            ignoreCancelled = true,
            priority = EventPriority.LOWEST
        ) {
            val player = entity as? Player ?: return@event
            val item = item ?: return@event
            val origin = player.origin()

            foodLevelChange(origin, item, player)
            origin.foodBlocks[item.type]?.invoke(player)
            origin.foodAttributes[item.type]?.forEach { it.invoke(player) }
            origin.foodPotions[item.type]?.invokeIfNotNull(player::addPotionEffects)
        }

        event<PlayerOriginChangeEvent>(
            ignoreCancelled = true,
            priority = EventPriority.LOWEST
        ) {
            val now = now()

            if (bypassOrEarly(now)) return@event cancel()
            if (!bypassCooldown) player.originTime = now

            newSuspendedTransaction { PlayerData[player.uniqueId].origin = newOrigin }
            Trigger.invokeReload(player, preOrigin, newOrigin) // TODO: This should cover the removeUnfulfilled method

            lang.origin.broadcast[
                "player" to { player.displayName() },
                "new_origin" to { newOrigin.displayName },
                "old_origin" to { preOrigin.displayName }
            ] message onlinePlayers

            if (config.showTitleOnChange) newOrigin.becomeOriginTitle?.invoke(player)
            removeUnfulfilledOrInvalidAttributes(player)
        }

        events(
            PlayerLeftClickEvent::class,
            PlayerRightClickEvent::class,
            PlayerOffhandEvent::class,
            PlayerDoubleLeftClickEvent::class,
            PlayerDoubleRightClickEvent::class,
            PlayerDoubleOffhandEvent::class,
            PlayerShiftDoubleLeftClickEvent::class,
            PlayerShiftDoubleRightClickEvent::class,
            PlayerShiftDoubleOffhandEvent::class,
            PlayerShiftLeftClickEvent::class,
            PlayerShiftRightClickEvent::class,
            PlayerShiftOffhandEvent::class,
        ) {
            val clazz = KeyBinding.fromEvent(this::class)
            val origin = player.origin()

            val ability = origin.abilities[clazz] ?: return@events
            ability.toggle(player)
        }

        val channels = hashMapOf<UUID, Channel<Boolean>>()
        val lasts = hashMapOf<UUID, Long>()
        event<PlayerRightClickEvent> {
            if (!this.hasItem) return@event
            var now = now().toEpochMilliseconds()

            val channel = channels.computeIfAbsent(player.uniqueId) {
                val channel = Channel<Boolean>(2) // 2 So we can overflow in the case of lag
                lasts[player.uniqueId] = now

                log.debug { "PlayerRightClickEvent - channel created with empty state ${channel.isEmpty}" }

                async {
                    var required = 1600L
                    do {
                        delay(400.milliseconds)

                        now = now().toEpochMilliseconds()
                        val received = channel.tryReceive()

                        when {
                            received.isSuccess -> {
                                log.debug { "PlayerRightClickEvent - received ${received.getOrNull()}" }
                                required -= (now - last)
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
                            break
                        }
//                        }
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

//            val serverPlayer = player.toNMS()

//            channel.

//            while ()

//            var index = 2000
//            while (index != 0) {
//                serverPlayer.usedItemHand
//                index--
//            }

//            val serverPlayer = player.toNMS()
//            val hand = serverPlayer.usedItemHand
//            val itemStack = serverPlayer.getItemInHand(hand)
//
//            if (serverPlayer.canEat(itemStack.item.foodProperties?.canAlwaysEat() == true)) {
//                serverPlayer.startUsingItem(InteractionHand.valueOf(hand.name))
//                InteractionResultHolder.consume(itemStack)
//            }
//
//            return if (user.canEat(
//                    this.getFoodProperties()
//                        .canAlwaysEat()
//                )
//            ) {
//                user.startUsingItem(hand)
//                InteractionResultHolder.consume(itemStack)
//            } else {
//                InteractionResultHolder.fail(itemStack)
//            }

            // foodEvent(player, item!!)
        }

        event<PlayerItemConsumeEvent> {
            log.debug { "PlayerItemConsumeEvent: $player, $item" }

//            foodEvent(player, item!!)
        }
    }

    private suspend fun foodEvent(
        player: Player,
        item: ItemStack
    ) {
        val serverPlayer = player.toNMS()
        val hand = serverPlayer.usedItemHand
        val itemStack = serverPlayer.getItemInHand(hand)
        var foodInfo = player.origin().customFoodProperties[item.type] ?: itemStack.item.foodProperties

        if (serverPlayer.canEat(itemStack.item.foodProperties?.canAlwaysEat() == true)) {
            serverPlayer.startUsingItem(InteractionHand.valueOf(hand.name))
            InteractionResultHolder.consume(itemStack)
        } else {
            InteractionResultHolder.fail(itemStack)
        }

        val origin = player.origin()

        val attributes = origin.foodAttributes[item.type]

        val actions = origin.foodBlocks[item.type]

        if (!item.type.isEdible && actions == null && attributes == null && foodInfo == null) return // Theres nothing to do here
        if (foodInfo == null) foodInfo = itemStack.item.foodProperties

        if (foodInfo != null) {
            val oldFoodLevel: Int = serverPlayer.foodData.foodLevel
            val event = CraftEventFactory.callFoodLevelChangeEvent(serverPlayer, foodInfo.nutrition + oldFoodLevel, itemStack)

            if (!event.isCancelled) {
                serverPlayer.foodData.eat(event.foodLevel - oldFoodLevel, foodInfo.saturationModifier)
                serverPlayer.awardStat(Stats.ITEM_USED[itemStack.item])
                CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, itemStack)

                attributes?.forEach { it.invoke(player) }
                actions?.invoke(player)
            }

            player.sendHealthUpdate()
        } else {
            attributes?.forEach { it.invoke(player) }
            actions?.invoke(player)
        }
    }

    private fun BeaconEffectEvent.shouldIgnore(): Boolean {
        val potion = player.getPotionEffect(effect.type) ?: return true
        return potion.duration < effect.duration ||
            !potion.hasKey() ||
            !potion.key!!.asString().matches(PotionEffectBuilder.regex)
    }

    // TODO -> Saturation
    private fun FoodLevelChangeEvent.foodLevelChange(
        origin: AbstractOrigin,
        item: ItemStack,
        player: Player
    ) {
//        origin.foodMultipliers[item.type]?.invokeIfNotNull {
//            cancel()
//            if (it == 0.0) return@invokeIfNotNull
//            player.foodLevel += ((foodLevel - player.foodLevel) * it).toInt()
//        }
    }

    private fun PlayerOriginChangeEvent.bypassOrEarly(now: Instant): Boolean {
        if (!bypassCooldown &&
            player.originTime + config.intervalBeforeChange > now
        ) return true
        return false
    }

    private fun ensureNoFire(
        player: Player,
        origin: AbstractOrigin,
    ): Boolean {
        if (player.fireTicks <= 0 || !origin.fireImmune) return false
        player.fireTicks = 0
        return true
    }

    private fun EntityPotionEffectEvent.shouldCancel() = entity is Player &&
        cause == EntityPotionEffectEvent.Cause.MILK &&
        oldEffect != null && oldEffect!!.hasKey() &&
        oldEffect!!.key!!.asString().matches(PotionEffectBuilder.regex)

    private fun removeUnfulfilledOrInvalidAttributes(
        player: Player,
        activeTriggers: List<Trigger> = player.activeTriggers()
    ) {
        for (attribute in Attribute.values()) {
            val inst = player.getAttribute(attribute) ?: continue

            for (modifier in inst.modifiers) {
                val match = AttributeModifierBuilder.regex.find(modifier.name)?.groups ?: continue
                val trigger = Trigger.valueOf(match["trigger"]!!.value.uppercase())

                if (trigger !in activeTriggers ||
                    match["origin"]!!.value != player.origin().name.lowercase()
                ) {
                    inst.removeModifier(modifier)
                    continue
                }

                // Make sure the attribute is unchanged
                player.origin().attributeModifiers[trigger]?.firstOrNull {
                    it.first == attribute &&
                        it.second.name == modifier.name &&
                        it.second.amount == modifier.amount &&
                        it.second.operation == modifier.operation &&
                        it.second.slot == modifier.slot
                } ?: inst.removeModifier(modifier)
            }
        }
    }

    private suspend fun timeTrigger(trigger: Trigger) {
        onlinePlayers
            .filter(Player::inOverworld)
            .onEach { player ->
                with(player.origin()) {
                    trigger.invokeSwap(if (trigger == Trigger.DAY) Trigger.NIGHT else Trigger.DAY, player, this)
                    titles[Trigger.DAY]?.invoke(player)
                }
            }
    }
}
