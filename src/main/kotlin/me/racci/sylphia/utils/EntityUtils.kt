package me.racci.sylphia.utils

import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Tameable

object TameUtils {

    fun isTamed(entity: Entity): Boolean {
        return entity is Tameable && entity.isTamed
    }

    fun isOwner(player: Player, entity: Entity): Boolean {
        return entity is Tameable && entity.isTamed && entity.ownerUniqueId == player.uniqueId
    }

}