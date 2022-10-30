package dev.racci.terix.core

import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.dsl.AttributeModifierBuilder
import dev.racci.terix.api.dsl.PotionEffectBuilder
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect

private fun Player.basePotions(): Sequence<Pair<PotionEffect, MatchResult>> {
    val origin = TerixPlayer.cachedOrigin(this)
    return this.activePotionEffects.asSequence()
        .filter(PotionEffect::hasKey)
        .mapNotNull { pot ->
            val match = PotionEffectBuilder.regex.find(pot.key!!.asString()) ?: return@mapNotNull null
            if (match.groups["origin"]!!.value != origin.name.lowercase()) return@mapNotNull null
            pot to match
        }
}

val Player.originPassivePotions: Sequence<PotionEffect> get() = this.basePotions()
    .filter { (_, match) -> match.groups["type"]!!.value == "potion" }
    .map { (pot, _) -> pot }

val Player.originAbilityPotions: Sequence<PotionEffect> get() = this.basePotions()
    .filter { (_, match) -> match.groups["type"]!!.value == "ability" }
    .map { (pot, _) -> pot }

val Player.originFoodPotions get() = this.basePotions()
    .filter { (_, match) -> match.groups["type"]!!.value == "food" }
    .map { (pot, _) -> pot }

val Player.allOriginPotions get() = this.activePotionEffects.asSequence()
    .filter(PotionEffect::hasKey)
    .filter { pot -> PotionEffectBuilder.regex.matches(pot.key!!.asString()) }

val Player.originPassiveModifiers get() = Attribute.values().asSequence()
    .mapNotNull { attr -> this.getAttribute(attr) }
    .associateWith { attr -> attr.modifiers.filter { mod -> mod.name.matches(AttributeModifierBuilder.regex) } }
    .filterValues { mods -> mods.isNotEmpty() }
