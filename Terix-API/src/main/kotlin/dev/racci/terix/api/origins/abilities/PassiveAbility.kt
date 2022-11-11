package dev.racci.terix.api.origins.abilities

import arrow.analysis.unsafeCall
import arrow.core.Option
import com.github.benmanes.caffeine.cache.Caffeine
import dev.racci.minix.api.annotations.RunAsync
import dev.racci.minix.api.extensions.SimpleKListener
import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.minix.api.extensions.unregisterListener
import dev.racci.terix.api.Terix
import dev.racci.terix.api.annotations.DispatcherContext
import dev.racci.terix.api.annotations.OriginEventSelector
import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.enums.EventSelector.TargetSelector.Companion.isCompatible
import dev.racci.terix.api.origins.origin.Origin
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import org.apiguardian.api.API
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.koin.core.component.inject
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.valueParameters

public abstract class PassiveAbility public constructor(
    public val abilityPlayer: Player,
    public val linkedOrigin: Origin,
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

    @API(status = API.Status.INTERNAL)
    public suspend fun register() {
        logger.debug { "Registering passive ability $this for player ${abilityPlayer.name}." }
        activateEvents()
        this.onActivate()
    }

    @API(status = API.Status.INTERNAL)
    public suspend fun unregister() {
        logger.debug { "Unregistering passive ability $this for player ${abilityPlayer.name}." }
        this.onDeactivate()
        listener.unregisterListener()
    }

    private fun activateEvents() {
        forwarders[this::class].forEach { func ->
            val selectorAnnotation = func.findAnnotation<OriginEventSelector>()!!
            val kClass = (func.extensionReceiverParameter?.type?.classifier ?: unsafeCall(func.valueParameters[0]).type.classifier).castOrThrow<KClass<Event>>()

            this.listener.event(
                kClass,
                getKoin().get<Terix>(),
                selectorAnnotation.priority,
                selectorAnnotation.ignoreCancelled,
                func.hasAnnotation<RunAsync>(),
            ) {
                Option.fromNullable(selectorAnnotation.selector(this).swap().orNull())
                    .filter { player -> player.uniqueId == abilityPlayer.uniqueId }
                    .tap { func.callSuspend(this@PassiveAbility, this) }
            }
        }
    }

    public companion object {
        private val forwarders = Caffeine.newBuilder()
            .build<KClass<out PassiveAbility>, ImmutableSet<KFunction<*>>> { clazz ->
                clazz.declaredMembers.asSequence()
                    .filterIsInstance<KFunction<*>>()
                    .filter { it.hasAnnotation<OriginEventSelector>() }
                    .associateWith { it.findAnnotation<OriginEventSelector>()!! }
                    .filter { (_, annotation) -> annotation.selector.selector is EventSelector.PlayerSelector } // We forward by the player not by origin.
                    .filter { (func, annotation) -> annotation.selector.selector.isCompatible(func) }.keys.toImmutableSet()
            }
    }
}
