package dev.racci.terix.core.services.runnables

import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.utils.getKoin
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.states.State
import dev.racci.terix.core.services.TickService
import org.bukkit.entity.Player
import kotlin.reflect.KProperty0

// May need mutex locks due to having 4 threads running at once
sealed class ChildTicker(
    val player: Player,
    val origin: Origin,
    private val state: State?,
    private val wasFunc: KProperty0<Boolean>?,
    private val isFunc: KProperty0<Boolean>?
) : WithPlugin<Terix> by getKoin().get<TickService>() {

    var isAlive: Boolean = false
    var upForAdoption: Boolean = true

    abstract suspend fun handleRun()

    open suspend fun shouldRun(): Boolean = true

    /** Initialisation */
    open suspend fun onBirth() = Unit

    /** Kills this child task. */
    open suspend fun onAbortion() = Unit

    suspend fun breakWater() {
        isAlive = true
        upForAdoption = false
        onBirth()
    }

    suspend fun coatHanger() {
        isAlive = false
        onAbortion()
    }

    suspend fun run() {
        this.tryToggle()
        if (this.shouldRun()) this.handleRun()
    }

    private suspend fun tryToggle() {
        if (this.state == null) return

        val isBool = isFunc!!.get()
        if (wasFunc!!.get() == isBool) return

        if (isBool) {
            this.state.activate(this.player, this.origin)
        } else this.state.deactivate(this.player, this.origin)
    }

    init {
        if (this.state != null && (this.wasFunc == null || this.isFunc == null)) {
            error("State is not null, was and is functions must be provided for a child coroutine.")
        }
    }
}
