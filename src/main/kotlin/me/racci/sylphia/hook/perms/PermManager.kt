package me.racci.sylphia.hook.perms

import java.util.UUID

interface PermManager {
    fun addPermission(uuid: UUID, permissions: List<String>)
    fun addPermission(uuid: UUID, permission: String)
    fun removePermission(uuid: UUID, permissions: List<String>)
    fun removePermission(uuid: UUID, permission: String)
}