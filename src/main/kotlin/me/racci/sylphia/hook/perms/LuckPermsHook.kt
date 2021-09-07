@file:Suppress("unused")
@file:JvmName("LuckPermsHook")
package me.racci.sylphia.hook.perms

import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.model.user.User
import net.luckperms.api.node.Node
import org.bukkit.entity.Player
import java.util.function.Consumer

class LuckPermsHook : PermManager {
    private val luckPerms: LuckPerms = LuckPermsProvider.get()

    override fun addPermission(player: Player, permission: String?) {
        luckPerms.userManager.modifyUser(player.uniqueId) { user: User ->
            user.data().add(
                Node.builder(
                    permission!!
                ).value(true).build()
            )
        }
    }

    override fun removePermission(player: Player, permission: String?) {
        luckPerms.userManager.modifyUser(player.uniqueId) { user: User ->
            user.data().remove(
                Node.builder(
                    permission!!
                ).value(true).build()
            )
        }
    }

    override fun addPermission(player: Player, permissions: List<String?>) {
        luckPerms.userManager.modifyUser(player.uniqueId) { user: User ->
            permissions.forEach(
                Consumer { perm: String? ->
                    user.data().add(
                        Node.builder(
                            perm!!
                        ).value(true).build()
                    )
                })
        }
    }

    override fun removePermission(player: Player, permissions: List<String?>) {
        luckPerms.userManager.modifyUser(player.uniqueId) { user: User ->
            permissions.forEach(
                Consumer { perm: String? ->
                    user.data().remove(
                        Node.builder(
                            perm!!
                        ).value(true).build()
                    )
                })
        }
    }

}