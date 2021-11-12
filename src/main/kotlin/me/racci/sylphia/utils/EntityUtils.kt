package me.racci.sylphia.utils

import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Tameable

object TamedUtils {

    fun isTamed(entity: Entity) =
        entity is Tameable && entity.isTamed

    fun isOwner(player: Player, entity: Entity) =
        entity is Tameable && entity.isTamed && entity.ownerUniqueId == player.uniqueId

}