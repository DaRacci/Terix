package dev.racci.terix.api.origins.abilities

import dev.racci.minix.api.extensions.SimpleKListener
import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.extensions.event
import dev.racci.terix.api.Terix
import dev.racci.terix.api.annotations.DispatcherContext
import dev.racci.terix.api.origins.origin.Origin
import org.apiguardian.api.API
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityEvent
import org.bukkit.event.player.PlayerEvent
import org.koin.core.component.inject

public abstract class PassiveAbility public constructor(
    protected val abilityPlayer: Player,
    protected val linkedOrigin: Origin,
) : WithPlugin<Terix> {
    final override val plugin: Terix by inject()

    @PublishedApi
    internal val listener: SimpleKListener = SimpleKListener(plugin)

    /** Called when the player gains this passive ability. */
    @DispatcherContext(DispatcherContext.Context.ASYNC)
    protected open suspend fun onActivate(): Unit = Unit

    /** Called when the player loses this passive ability. */
    @DispatcherContext(DispatcherContext.Context.ASYNC)
    protected open suspend fun onDeactivate(): Unit = Unit

    protected inline fun <reified T : EntityEvent> subscribe(
        priority: EventPriority = EventPriority.HIGHEST,
        ignoreCancelled: Boolean = false,
        forceAsync: Boolean = false,
        noinline handler: suspend T.() -> Unit
    ): Unit = listener.event<T>(plugin, priority, ignoreCancelled, forceAsync) {
        if (this is PlayerEvent && player === abilityPlayer || this.entity === abilityPlayer) handler(this)
    }

    @API(status = API.Status.INTERNAL)
    public suspend fun register() {
        this.onActivate()
    }

    @API(status = API.Status.INTERNAL)
    public suspend fun unregister() {
        this.onDeactivate()
    }
}