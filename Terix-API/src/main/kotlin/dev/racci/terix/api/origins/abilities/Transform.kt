package dev.racci.terix.api.origins.abilities

import dev.racci.minix.api.collections.PlayerMap
import me.libraryaddict.disguise.DisguiseAPI
import me.libraryaddict.disguise.disguisetypes.Disguise
import org.bukkit.entity.Player

class Transform : Ability(AbilityType.TOGGLE) {
    val disguises = PlayerMap<Disguise>()

    override suspend fun onActivate(player: Player) {
        disguises[player]?.let { DisguiseAPI.disguiseEntity(player, it) }
    }

    override suspend fun onDeactivate(player: Player) {
        DisguiseAPI.undisguiseToAll(player)
    }
}
