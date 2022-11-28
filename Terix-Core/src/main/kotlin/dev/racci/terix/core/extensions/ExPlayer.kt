package dev.racci.terix.core.extensions

import dev.racci.terix.api.extensions.allOriginPotions
import dev.racci.terix.api.extensions.originPassiveModifiers
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect

public fun Player.sanitise() {
    allOriginPotions.map(PotionEffect::getType).forEach(this::removePotionEffect)
    originPassiveModifiers.forEach { it.value.forEach(it.key::removeModifier) }
}

public fun Player.inDarkness(): Boolean = inventory.itemInMainHand.type != Material.TORCH &&
    inventory.itemInOffHand.type != Material.TORCH &&
    location.block.lightLevel < 5

public infix fun Component.message(receiver: Collection<Player>) { for (audience in receiver) { audience.sendMessage(this) } }
