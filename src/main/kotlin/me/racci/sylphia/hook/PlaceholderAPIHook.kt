package me.racci.sylphia.hook

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import me.racci.sylphia.Sylphia
import me.racci.sylphia.data.PlayerManager
import me.racci.sylphia.origins.OriginManager
import org.bukkit.entity.Player

class PlaceholderAPIHook : PlaceholderExpansion() {

    override fun persist() = true
    override fun canRegister() = true

    override fun getAuthor() = Sylphia.instance.description.authors.joinToString(", ")
    override fun getVersion() = Sylphia.instance.description.version
    override fun getIdentifier() = Sylphia.instance.description.name

    override fun onPlaceholderRequest(player: Player, params: String): String {
        return when (params.lowercase()) {
            "origin" -> OriginManager.getOrigin(player)?.identity?.displayName ?: ""
            "lastorigin" -> OriginManager.valueOf(PlayerManager[player.uniqueId].lastOrigin ?: return "").identity.displayName
            else -> ""
        }
    }
}