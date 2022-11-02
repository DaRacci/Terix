package dev.racci.terix.api.origins.abilities

import dev.racci.terix.api.origins.origin.Origin
import me.libraryaddict.disguise.DisguiseAPI
import me.libraryaddict.disguise.disguisetypes.Disguise
import org.bukkit.entity.Player

public class Transform(override val origin: Origin) : Ability(AbilityType.TOGGLE) {
    public lateinit var disguise: Disguise

    override suspend fun onActivate(player: Player) {
        DisguiseAPI.disguiseEntity(player, disguise)
    }

    override suspend fun onDeactivate(player: Player) {
        DisguiseAPI.undisguiseToAll(player)
    }
}
