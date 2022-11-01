package dev.racci.terix.api.origins.abilities

import dev.racci.minix.api.collections.PlayerMap
import me.libraryaddict.disguise.DisguiseAPI
import me.libraryaddict.disguise.disguisetypes.Disguise
import org.bukkit.entity.Player

public class Transform : Ability(AbilityType.TOGGLE) {
    public val disguises: PlayerMap<Disguise> = PlayerMap()

    override suspend fun onActivate(player: Player) {
        disguises[player]?.let { DisguiseAPI.disguiseEntity(player, it) }
    }

    override suspend fun onDeactivate(player: Player) {
        DisguiseAPI.undisguiseToAll(player)
    }
}
