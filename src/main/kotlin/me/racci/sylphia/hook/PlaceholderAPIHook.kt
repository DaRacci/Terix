@file:Suppress("unused")
@file:JvmName("PlaceholderAPIHook")
package me.racci.sylphia.hook

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import me.racci.sylphia.Sylphia
import me.racci.sylphia.origins.OriginManager.Origins
import org.bukkit.entity.Player

class PlaceholderAPIHook(private val plugin: Sylphia) : PlaceholderExpansion() {

    override fun persist(): Boolean {
        return true
    }

    override fun canRegister(): Boolean {
        return true
    }

    override fun getIdentifier(): String {
        return "sylphia"
    }

    override fun getAuthor(): String {
        return "Racci"
    }

    override fun getVersion(): String {
        return "Alpha-0.1"
    }

    override fun onPlaceholderRequest(player: Player, params: String): String? {
        if (params.equals("origin", ignoreCase = true)) {
            return plugin.originManager!!.getOrigin(player)?.displayName
        }
        return if (params.equals("lastOrigin", ignoreCase = true)) {
            Origins.valueOf(
                plugin.playerManager!!.getPlayerData(
                    player.uniqueId
                )?.lastOrigin!!
            ).displayName
        } else null
    }
}