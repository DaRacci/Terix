package dev.racci.terix.core

import dev.racci.minix.api.annotations.MappedPlugin
import dev.racci.minix.api.extensions.onlinePlayers
import dev.racci.terix.api.Terix
import dev.racci.terix.api.data.player.TerixPlayer
import dev.racci.terix.api.origins.states.State
import dev.racci.terix.core.integrations.TempPlaceholderExpansion
import org.incendo.interfaces.paper.PaperInterfaceListeners
import xyz.xenondevs.particle.utils.ReflectionUtils

@MappedPlugin(14443, Terix::class)
public class TerixImpl : Terix() {
    override suspend fun handleLoad() {
        TempPlaceholderExpansion(this).register()
    }

    override suspend fun handleEnable() {
        PaperInterfaceListeners.install(this)
        ReflectionUtils.setPlugin(this)
    }

    override suspend fun handleDisable() {
        onlinePlayers.map(TerixPlayer::get)
            .associateWith(State::get)
            .forEach { (player, state) -> state.forEach { it.deactivate(player.backingPlayer, player.origin) } }
    }
}
