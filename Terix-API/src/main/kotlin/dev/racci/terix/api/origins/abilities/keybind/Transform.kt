package dev.racci.terix.api.origins.abilities.keybind

import dev.racci.terix.api.data.player.TerixPlayer
import dev.racci.terix.api.origins.origin.Origin
import me.libraryaddict.disguise.DisguiseAPI
import me.libraryaddict.disguise.disguisetypes.Disguise
import kotlin.time.Duration

public class Transform(
    override val abilityPlayer: TerixPlayer,
    override val linkedOrigin: Origin,
    override val cooldownDuration: Duration,
    public val disguise: Disguise
) : TogglingKeybindAbility() {

    override suspend fun handleActivation() {
        DisguiseAPI.setActionBarShown(abilityPlayer, false)
        sync { DisguiseAPI.disguiseToAll(abilityPlayer, disguise) }
    }

    override suspend fun handleDeactivation() {
        sync { DisguiseAPI.undisguiseToAll(abilityPlayer) }
        DisguiseAPI.setActionBarShown(abilityPlayer, true)
    }
}
