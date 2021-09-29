package me.racci.sylphia.handlers

import me.racci.raccicore.skedule.skeduleSync
import me.racci.sylphia.enums.Condition
import me.racci.sylphia.extensions.PlayerExtension.currentOrigin
import me.racci.sylphia.sylphia
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect

object PotionHandler {

    private fun getCondition(player: Player, condition: Condition) : HashSet<PotionEffect> {
        return player.currentOrigin?.effectMap?.get(condition) ?: return HashSet()
    }

    fun setCondition(player: Player, condition: Condition) {
        skeduleSync(sylphia) {player.addPotionEffects(getCondition(player,condition))}
    }

    fun removeCondition(player: Player, condition: Condition) {
        skeduleSync(sylphia) {
            for (potion in getCondition(player, condition)) {
                if (player.hasPotionEffect(potion.type) &&
                    player.getPotionEffect(potion.type)!!.extra == condition
                ) {
                    player.removePotionEffect(potion.type)
                }
            }
        }
    }

    fun hasCondition(player: Player, condition: Condition) : Boolean {
        for (potion in getCondition(player, condition)) {
            if (!player.hasPotionEffect(potion.type) ||
                player.getPotionEffect(potion.type)!!.extra != condition
            ) {
                return false
            }
        }
        return true
    }

}