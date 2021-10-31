package me.racci.sylphia.hook.perms

import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.node.Node
import java.util.UUID

class LuckPermsHook : PermManager {
    private val luckPerms: LuckPerms = LuckPermsProvider.get()

    override fun addPermission(uuid: UUID, permission: String) {
        luckPerms.userManager.modifyUser(uuid) {
            it.data().add(
                Node.builder(
                    permission
                ).value(true).build()
            )
        }
    }

    override fun removePermission(uuid: UUID, permission: String) {
        luckPerms.userManager.modifyUser(uuid) {
            it.data().remove(
                Node.builder(
                    permission
                ).value(true).build()
            )
        }
    }

    override fun addPermission(uuid: UUID, permissions: List<String>) {
        luckPerms.userManager.modifyUser(uuid) { u ->
            permissions.forEach{
                u.data().add(
                    Node.builder(
                        it
                    ).value(true).build()
                )
            }
        }
    }

    override fun removePermission(uuid: UUID, permissions: List<String>) {
        luckPerms.userManager.modifyUser(uuid) { u ->
            permissions.forEach {
                u.data().remove(
                    Node.builder(
                        it
                    ).value(true).build()
                )
            }
        }
    }
}