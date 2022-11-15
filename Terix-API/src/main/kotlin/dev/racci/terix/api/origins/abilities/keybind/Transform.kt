package dev.racci.terix.api.origins.abilities.keybind

import dev.racci.terix.api.origins.origin.Origin
import me.libraryaddict.disguise.DisguiseAPI
import me.libraryaddict.disguise.disguisetypes.Disguise
import org.bukkit.entity.Player

public class Transform(
    override val abilityPlayer: Player,
    override val linkedOrigin: Origin
) : TogglingKeybindAbility() {
    public lateinit var disguise: Disguise

    override suspend fun handleActivation() {
        sync { DisguiseAPI.disguiseToAll(abilityPlayer, disguise) }
    }

    override suspend fun handleDeactivation() {
        DisguiseAPI.undisguiseToAll(abilityPlayer)
    }
}
