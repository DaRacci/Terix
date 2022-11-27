package dev.racci.terix.core.services.runnables

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.utils.getKoin
import dev.racci.terix.api.Terix
import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.TerixPlayer.TickCache
import dev.racci.terix.api.TerixPlayer.TickCache.TwoStateCache
import dev.racci.terix.api.origins.states.State
import dev.racci.terix.api.services.TickService
import kotlin.reflect.KProperty1

// TODO -> May need mutex locks due to having 4 threads running at once
public sealed class ChildTicker private constructor(
    protected val player: TerixPlayer,
    private val state: Option<State>,
    twoState: Option<KProperty1<TickCache, TwoStateCache>>
) : WithPlugin<Terix> by getKoin().get<TickService>() {
    protected constructor(terixPlayer: TerixPlayer) : this(terixPlayer, None, None)

    protected constructor(terixPlayer: TerixPlayer, state: State) : this(terixPlayer, Some(state), None)

    protected constructor(terixPlayer: TerixPlayer, state: State, twoState: KProperty1<TickCache, TwoStateCache>) : this(terixPlayer, Some(state), Some(twoState))

    private val twoState = twoState.map { it.get(player.ticks) }

    public var isAlive: Boolean = false
    public var upForAdoption: Boolean = true

    public abstract suspend fun handleRun()

    public open suspend fun shouldRun(): Boolean = true

    /** Initialisation */
    public open suspend fun onBirth(): Unit = Unit

    /** Kills this child task. */
    public open suspend fun onAbortion(): Unit = Unit

    public suspend fun breakWater() {
        isAlive = true
        upForAdoption = false
        onBirth()
    }

    public suspend fun coatHanger() {
        isAlive = false
        onAbortion()
    }

    public suspend fun run() {
        this.tryToggle()
        if (this.shouldRun()) this.handleRun()
    }

    private suspend fun tryToggle() {
        if (this.state !is Some || this.twoState !is Some) return

        val state = twoState.value
        val isBool = state.last()

        if (state.last() == isBool) return

        if (isBool) {
            this.state.value.activate(this.player, this.player.origin)
        } else this.state.value.deactivate(this.player, this.player.origin)
    }

    init {
        if (this.state is Some && this.twoState is None) {
            error("State is not null, was and is functions must be provided for a child coroutine.")
        }
    }
}
