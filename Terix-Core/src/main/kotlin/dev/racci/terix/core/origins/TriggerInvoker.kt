package dev.racci.terix.core.origins

import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.Trigger
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

fun Trigger.invokeAdd(player: Player) {
    val origin = player.origin()

    origin.potions[this]?.let(player::safelyAddPotions)
    origin.attributeModifiers[this]?.let(player::addAttributeModifiers)
}

fun Trigger.Companion.invokeAddAll(
    player: Player,
    origin: AbstractOrigin = player.origin(),
    triggers: Collection<Trigger>
) {
    val potions = mutableListOf<PotionEffect>()
    val attributeModifiers = mutableListOf<Pair<Attribute, AttributeModifier>>()

    for (trigger in triggers) {
        origin.potions[trigger]?.let(potions::addAll)
        origin.attributeModifiers[trigger]?.let(attributeModifiers::addAll)
    }

    player.safelyAddPotions(potions)
    player.addAttributeModifiers(attributeModifiers)
}

fun Trigger.invokeSwap(
    oldTrigger: Trigger,
    player: Player,
    origin: AbstractOrigin = player.origin()
) {
    val removePotions = player.getAllOriginPotions(oldTrigger)
    val removeModifiers = origin.attributeModifiers[oldTrigger]
    val addPotions = origin.potions[this]
    val addModifiers = origin.attributeModifiers[this]

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

fun Trigger.invokeRemove(player: Player, origin: AbstractOrigin = player.origin()) {
    val potions = player.getAllOriginPotions(this)

    player.safelyRemovePotions(potions)

    origin.attributeModifiers[this]?.let(player::removeAttributeModifiers)
}

fun Trigger.Companion.invokeCompleteClean(player: Player, origin: AbstractOrigin = player.origin()) {
    val potions = player.getAllOriginPotions()

    player.safelyRemovePotions(potions)

    for ((attribute, _) in origin.attributeBase) {
        player.getAttribute(attribute)?.let { it.baseValue = it.defaultValue }
    }

    for (collection in origin.attributeModifiers.values.filterNotNull()) {
        player.removeAttributeModifiers(collection)
    }
}

fun Trigger.Companion.invokeBase(player: Player, origin: AbstractOrigin = player.origin()) {
    val basePotions = origin.potions[Trigger.ON]
    val baseAttributes = origin.attributeBase

    basePotions?.let { player.safelyAddPotions(it) }
    for ((attribute, value) in baseAttributes) {
        player.getAttribute(attribute)?.baseValue = value
    }
}

fun Trigger.Companion.invokeReload(
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
    activePotionEffects.filterNot { effect ->
        !effect.hasKey() || effect.key!!.namespace != "origin" ||
            (trigger != null && effect.key!!.key != trigger.name.lowercase())
    }.map(PotionEffect::getType)

private fun Player.removeAttributeModifiers(collection: Collection<Pair<Attribute, AttributeModifier>>) {
    for ((attribute, modifier) in collection) {
        getAttribute(attribute)?.removeModifier(modifier)
    }
}

private fun Player.addAttributeModifiers(collection: Collection<Pair<Attribute, AttributeModifier>>) {
    for ((attribute, modifier) in collection) {
        getAttribute(attribute)?.addModifier(modifier)
    }
}
