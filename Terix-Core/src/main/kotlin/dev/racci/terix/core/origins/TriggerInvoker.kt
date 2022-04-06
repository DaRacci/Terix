package dev.racci.terix.core.origins

import dev.racci.minix.api.extensions.msg
import dev.racci.minix.api.utils.kotlin.ifNotEmpty
import dev.racci.minix.api.utils.kotlin.ifTrue
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.core.data.PlayerData
import dev.racci.terix.core.extension.getLightOrDarkTrigger
import dev.racci.terix.core.extension.origin
import dev.racci.terix.core.extension.safelyAddPotions
import dev.racci.terix.core.extension.safelyRemovePotions
import dev.racci.terix.core.extension.safelySwapPotions
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.time.Duration

suspend fun Trigger.invokeAdd(
    player: Player,
    origin: AbstractOrigin = player.origin()
) {
    val potions = origin.potions[this] ?: mutableListOf()

    if (origin.nightVision) {
        potions += PotionEffectBuilder {
            type = PotionEffectType.NIGHT_VISION
            duration = Duration.INFINITE
            amplifier = 0
            ambient = true
            originKey(origin, this@invokeAdd)
        }.build()
    }

    origin.titles[this]?.invoke(player)
    origin.triggerBlocks[this]?.invoke(player)

    potions.ifNotEmpty(player::safelyAddPotions)
    origin.attributeModifiers[this]?.let(player::addAttributeModifiers)
}

suspend fun Trigger.Companion.invokeAddAll(
    player: Player,
    origin: AbstractOrigin = player.origin(),
    triggers: Collection<Trigger>
) {
    val potions = mutableListOf<PotionEffect>()
    val attributeModifiers = mutableListOf<Pair<Attribute, AttributeModifier>>()

    for (trigger in triggers) {
        origin.titles[trigger]?.invoke(player)
        origin.triggerBlocks[trigger]?.invoke(player)

        origin.potions[trigger]?.let(potions::addAll)
        origin.attributeModifiers[trigger]?.let(attributeModifiers::addAll)
    }

    origin.nightVision.ifTrue {
        val nightVision = transaction { PlayerData[player].nightVision }
        if (nightVision !in triggers) return@ifTrue
        PotionEffectBuilder {
            type = PotionEffectType.NIGHT_VISION
            duration = Duration.INFINITE
            amplifier = 0
            ambient = true
            originKey(origin, nightVision)
        }.build().apply { potions += this }
    }

    player.safelyAddPotions(potions)
    player.addAttributeModifiers(attributeModifiers)
}

suspend fun Trigger.invokeSwap(
    oldTrigger: Trigger,
    player: Player,
    origin: AbstractOrigin = player.origin()
) {
    val removePotions = player.getAllOriginPotions(oldTrigger)
    val removeModifiers = origin.attributeModifiers[oldTrigger]
    var addPotions = origin.potions[this]?.toMutableList()
    val addModifiers = origin.attributeModifiers[this]

    origin.titles[this]?.invoke(player)
    origin.triggerBlocks[this]?.invoke(player)

    if (origin.nightVision && transaction { PlayerData[player].nightVision == this@invokeSwap }) {
        PotionEffectBuilder {
            type = PotionEffectType.NIGHT_VISION
            duration = Duration.INFINITE
            amplifier = 0
            ambient = true
            originKey(origin, this@invokeSwap)
        }.build().apply { addPotions?.add(this) ?: run { addPotions = mutableListOf(this) } }
    }

    player.safelySwapPotions(removePotions, addPotions)
    removeModifiers?.let(player::removeAttributeModifiers)
    addModifiers?.let(player::addAttributeModifiers)
}

fun Trigger.Companion.invokeRemovalFor(
    player: Player,
    vararg triggers: Trigger
) {
    val potions = mutableListOf<PotionEffectType>()
    val attributeModifiers = mutableListOf<Pair<Attribute, AttributeModifier>>()
    val origin = player.origin()

    for (trigger in triggers) {
        potions.addAll(player.getAllOriginPotions(trigger))
        origin.attributeModifiers[trigger]?.let(attributeModifiers::addAll)
    }

    player.safelyRemovePotions(potions)
    player.removeAttributeModifiers(attributeModifiers)
}

fun Trigger.invokeRemove(
    player: Player,
    origin: AbstractOrigin = player.origin()
) {
    val potions = player.getAllOriginPotions(this)

    player.safelyRemovePotions(potions)

    origin.attributeModifiers[this]?.let(player::removeAttributeModifiers)
}

fun Trigger.Companion.invokeCompleteClean(
    player: Player,
    origin: AbstractOrigin = player.origin()
) {
    // These shouldn't be needed but are here just in case.
    player.setCanBreathUnderwater(false)
    player.setImmuneToFire(false)

    val potions = player.getAllOriginPotions()
    println("Potions being removed: ${potions.map { it.name }}")

    player.safelyRemovePotions(potions)

    Attribute.values().forEach { attribute ->
        player.getAttribute(attribute)?.let { inst ->
            inst.modifiers.filter {
                it.name.startsWith("origin_modifier_").ifTrue {
                    player.msg("Removing modifier: ${it.name}")
                }
            }.forEach(inst::removeModifier)
        }
    }

    for (collection in origin.attributeModifiers.values.filterNotNull()) {
        println("Removing ${collection.size} modifiers")
        player.removeAttributeModifiers(collection)
    }
}

fun Trigger.Companion.invokeBase(
    player: Player,
    origin: AbstractOrigin = player.origin()
) {
    var basePotions = origin.potions[Trigger.ON]

    player.setCanBreathUnderwater(origin.waterBreathing)
    player.setImmuneToFire(origin.fireImmune)

    if (origin.nightVision && transaction { PlayerData[player].nightVision == Trigger.ON }) {
        PotionEffectBuilder {
            type = PotionEffectType.NIGHT_VISION
            duration = Duration.INFINITE
            amplifier = 0
            ambient = true
            originKey(origin.name.lowercase(), "on")
        }.build().apply { basePotions?.add(this) ?: run { basePotions = mutableListOf(this) } }
    }

    basePotions?.let { player.safelyAddPotions(it) }
    origin.attributeModifiers[Trigger.ON]?.let(player::addAttributeModifiers)
}

suspend fun Trigger.Companion.invokeReload(
    player: Player,
    oldOrigin: AbstractOrigin? = null,
    newOrigin: AbstractOrigin = player.origin()
) {
    invokeCompleteClean(player, oldOrigin ?: newOrigin)
    invokeBase(player, newOrigin)

    val triggers = mutableListOf<Trigger>()
    triggers += player.world.environment.getTrigger()
    player.world.getTimeTrigger()?.let(triggers::add)
    player.getLiquidTrigger().let(triggers::addAll)
    player.getLightOrDarkTrigger()?.let(triggers::add)

    Trigger.invokeAddAll(player, newOrigin, triggers)
}

private fun Player.getAllOriginPotions(trigger: Trigger? = null): Collection<PotionEffectType> =
    activePotionEffects.filter { effect ->
        effect.hasKey() && PotionEffectBuilder.regex.find(effect.key!!.asString())?.let { match ->
            return@let if (trigger != null) {
                match.groups[2]?.value == trigger.name.lowercase()
            } else true
        } == true
    }.map(PotionEffect::getType)

private fun Player.removeAttributeModifiers(collection: Collection<Pair<Attribute, AttributeModifier>>) {
    for ((attribute, modifier) in collection) {
        println("Removing modifier ${modifier.serialize()}")
        getAttribute(attribute)?.removeModifier(modifier)
    }
}

private fun Player.addAttributeModifiers(collection: Collection<Pair<Attribute, AttributeModifier>>) {
    for ((attribute, modifier) in collection) {
        println("Adding modifier ${modifier.serialize()}")
        getAttribute(attribute)?.addModifier(modifier)
    }
}
