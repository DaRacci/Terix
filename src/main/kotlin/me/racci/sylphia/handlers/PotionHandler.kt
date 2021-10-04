package me.racci.sylphia.handlers

import me.racci.sylphia.enums.Condition
import me.racci.sylphia.extensions.PlayerExtension.currentOrigin
import me.racci.sylphia.factories.Origin
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect

object PotionHandler {

    private fun getCondition(player: Player, condition: Condition, origin: Origin? = player.currentOrigin) : ArrayList<PotionEffect> {
        return origin?.potions?.get(condition) ?: ArrayList(0)
    }

    fun setBase(player: Player, origin: Origin? = player.currentOrigin) {
        origin?.potions?.get(Condition.PARENT).apply {
            if(this == null) return else player.addPotionEffects(this)
        }
    }

    fun setCondition(player: Player, condition: Condition, origin: Origin? = player.currentOrigin) {
        origin?.potions?.get(condition).apply {
            if(this == null) return else player.addPotionEffects(this)
        }
    }

    fun removeCondition(player: Player, condition: Condition, origin: Origin? = player.currentOrigin) {
        for (potion in getCondition(player, condition, origin)) {
            if (player.hasPotionEffect(potion.type) &&
                player.getPotionEffect(potion.type)!!.extra == condition
            ) {
                player.removePotionEffect(potion.type)
            }
        }
    }

    fun hasCondition(player: Player, condition: Condition, origin: Origin? = player.currentOrigin) : Boolean {
        for (potion in getCondition(player, condition, origin)) {
            if (!player.hasPotionEffect(potion.type) ||
                player.getPotionEffect(potion.type)!!.extra != condition
            ) {
                return false
            }
        }
        return true
    }

    fun reset(player: Player) {
        for(potion in player.activePotionEffects) {
            if(potion.hasExtra()) {
                player.removePotionEffect(potion.type)
            }
        }
    }

}