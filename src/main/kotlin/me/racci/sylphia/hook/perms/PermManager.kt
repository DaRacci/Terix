package me.racci.sylphia.hook.perms

import org.bukkit.entity.Player

interface PermManager {
    fun addPermission(player: Player, permissions: List<String?>)
    fun addPermission(player: Player, permission: String?)
    fun removePermission(player: Player, permissions: List<String?>)
    fun removePermission(player: Player, permission: String?)
}