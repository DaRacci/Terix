package dev.racci.terix.core.origins

import dev.racci.minix.api.utils.getKoin
import dev.racci.terix.api.Terix
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.ensureMainThread
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.core.extensions.activeTriggers
import dev.racci.terix.core.extensions.fromOrigin
import dev.racci.terix.core.extensions.nightVision
import dev.racci.terix.core.extensions.origin
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.time.Duration

suspend fun Trigger.invokeAdd(
    player: Player,
    origin: AbstractOrigin = player.origin()
) {
    getKoin().get<Terix>().log.debug { "Activating Trigger: $name" }
    applyAsyncable(player, origin, this, false)
    applyMainThread(player, origin, this, false)
}

/* suspend fun Trigger.Companion.invokeAddAll(
    player: Player,
    origin: AbstractOrigin = player.origin(),
    triggers: Collection<Trigger>
) {
    val potions = mutableListOf<PotionEffect>()

    for (trigger in triggers) {
        applyAsyncable(player, origin, trigger, false)
        origin.potions[trigger]?.let(potions::addAll)
    }

    ensureMainThread { triggers.forEach { applyMainThread(player, origin, it, false) } }
} */

suspend fun Trigger.invokeSwap(
    oldTrigger: Trigger,
    player: Player,
    origin: AbstractOrigin = player.origin()
) {
    getKoin().get<Terix>().log.debug { "Swapping Trigger from $oldTrigger to $this" }
    applyAsyncable(player, origin, oldTrigger, true)
    applyAsyncable(player, origin, this, false)
    ensureMainThread {
        applyMainThread(player, origin, oldTrigger, true)
        applyMainThread(player, origin, this, false)
    }
}

suspend fun Trigger.invokeRemove(
    player: Player,
    origin: AbstractOrigin = player.origin()
) {
    getKoin().get<Terix>().log.debug { "Deactivating Trigger: $name" }
    applyAsyncable(player, origin, this, true)
    applyMainThread(player, origin, this, true)
}

suspend fun Trigger.Companion.invokeBase(
    player: Player,
    origin: AbstractOrigin = player.origin()
) {
    getKoin().get<Terix>().log.debug { "Adding base" }
    applyAsyncable(player, origin, Trigger.ON, false)
    applyMainThread(player, origin, Trigger.ON, false)

    player.setCanBreathUnderwater(origin.waterBreathing)
    player.setImmuneToFire(origin.fireImmune)
}

suspend fun Trigger.Companion.invokeReload(
    player: Player,
    oldOrigin: AbstractOrigin? = null,
    newOrigin: AbstractOrigin = player.origin()
) {
    getKoin().get<Terix>().log.debug { "Changing origins from ${oldOrigin?.name ?: "null"} to ${newOrigin.name}" }

    player.setImmuneToFire(newOrigin.fireImmune)
    player.setCanBreathUnderwater(newOrigin.waterBreathing)

    val activeTriggers = player.activeTriggers()
    val removePotions = arrayListOf<PotionEffectType>()
    val curHealth = player.health

    if (oldOrigin != null) {
        activeTriggers.forEach { applyAsyncable(player, oldOrigin, it, true) }
        activeTriggers.map(player::getAllOriginPotions).forEach(removePotions::addAll)
    }

    ensureMainThread {
        removePotions.forEach(player::removePotionEffect)
        activeTriggers.forEach { newOrigin.potions[it]?.let(player::addPotionEffects) }
        when {
            oldOrigin?.nightVision == true && !newOrigin.nightVision -> {
                removePotions += PotionEffectType.NIGHT_VISION
            }
            oldOrigin?.nightVision != true && newOrigin.nightVision -> {
                val trigger = activeTriggers.find { it == player.nightVision } ?: return@ensureMainThread
                player.addPotionEffect(nightVisionPotion(newOrigin, trigger))
            }
        }
    }

    activeTriggers.forEach { applyAsyncable(player, newOrigin, it, false) }
    if (player.health < curHealth) player.health = curHealth.coerceAtMost(player.maxHealth)
}

fun Player.getAllOriginPotions(trigger: Trigger? = null): Collection<PotionEffectType> =
    activePotionEffects
        .filter { effect ->
            effect.hasKey() && PotionEffectBuilder.regex.find(effect.key!!.asString())?.let { match ->
                match.groups["type"]!!.value == "potion" &&
                    trigger == null || match.groups["trigger"]!!.value == trigger!!.name.lowercase()
            } ?: false
        }.map(PotionEffect::getType)

internal suspend fun applyAsyncable(
    player: Player,
    origin: AbstractOrigin,
    trigger: Trigger,
    remove: Boolean
) {
    if (remove) {
        origin.attributeModifiers[trigger]?.let(player::removeAttributeModifiers)
        return
    }
    origin.titles[trigger]?.invoke(player)
    origin.triggerBlocks[trigger]?.invoke(player)
    origin.attributeModifiers[trigger]?.let(player::addAttributeModifiers)
}

// TODO: Cache if the player has their nightvision potion
internal suspend fun applyMainThread(
    player: Player,
    origin: AbstractOrigin,
    trigger: Trigger,
    remove: Boolean
) = ensureMainThread {
    if (remove) {
        origin.potions[trigger]?.takeUnless(Collection<*>::isEmpty)?.map(PotionEffect::getType)?.forEach(player::removePotionEffect)
        if (origin.nightVision &&
            player.nightVision == trigger &&
            player.activePotionEffects.find { it.type == PotionEffectType.NIGHT_VISION }?.fromOrigin() == true
        ) player.removePotionEffect(PotionEffectType.NIGHT_VISION)
        return@ensureMainThread
    }
    origin.potions[trigger]?.takeUnless(Collection<*>::isEmpty)?.let(player::addPotionEffects)
    if (origin.nightVision &&
        player.nightVision == trigger &&
        player.activePotionEffects.find { it.type == PotionEffectType.NIGHT_VISION }?.fromOrigin() != true
    ) { player.addPotionEffect(nightVisionPotion(origin, trigger)) }
}

private fun nightVisionPotion(
    origin: AbstractOrigin,
    trigger: Trigger
) = PotionEffectBuilder {
    type = PotionEffectType.NIGHT_VISION
    duration = Duration.INFINITE
    amplifier = 0
    ambient = true
    originKey(origin, trigger)
}.build()

internal fun Player.removeAttributeModifiers(collection: Collection<Pair<Attribute, AttributeModifier>>) {
    for ((attribute, modifier) in collection) {
        getKoin().get<Terix>().log.debug { "Removing attribute modifier ${modifier.name} to ${attribute.name}" }
        getAttribute(attribute)?.removeModifier(modifier)
    }
}

internal fun Player.addAttributeModifiers(collection: Collection<Pair<Attribute, AttributeModifier>>) {
    for ((attribute, modifier) in collection) {
        getKoin().get<Terix>().log.debug { "Adding attribute modifier ${modifier.name} to ${attribute.name}" }
        getAttribute(attribute)?.addModifier(modifier)
    }
}
