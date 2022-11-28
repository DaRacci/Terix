package dev.racci.terix.api.origins.abilities

import arrow.core.Option
import com.github.benmanes.caffeine.cache.Caffeine
import dev.racci.minix.api.annotations.RunAsync
import dev.racci.minix.api.coroutine.launchAsync
import dev.racci.minix.api.coroutine.scope
import dev.racci.minix.api.extensions.SimpleKListener
import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.extensions.collections.clear
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.minix.api.extensions.unregisterListener
import dev.racci.terix.api.Terix
import dev.racci.terix.api.annotations.DispatcherContext
import dev.racci.terix.api.annotations.OriginEventSelector
import dev.racci.terix.api.data.player.TerixPlayer
import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.enums.EventSelector.TargetSelector.Companion.isCompatible
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.services.TickService
import io.ktor.util.collections.ConcurrentSet
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryLevel
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.plus
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

public abstract class Ability : WithPlugin<Terix> {
    private val abilityJobs = ConcurrentSet<Job>()

    @PublishedApi
    internal val listener: SimpleKListener by lazy { SimpleKListener(plugin) }

    final override val plugin: Terix by inject()

    public open val name: String = this::class.simpleName ?: throw IllegalStateException("KeybindAbility name is null")

    /** The player who this ability belongs to. */
    public abstract val abilityPlayer: TerixPlayer

    /** The origin instance that this ability belongs to. */
    public abstract val linkedOrigin: Origin

    /** Called when the player gains this ability. */
    @DispatcherContext(DispatcherContext.Context.ASYNC)
    protected open suspend fun handleAbilityGained(): Unit = Unit

    /** Called when the player looses this ability. */
    @DispatcherContext(DispatcherContext.Context.ASYNC)
    protected open suspend fun handleAbilityLost(): Unit = Unit

    internal open suspend fun handleInternalGained(): Unit = Unit

    internal open suspend fun handleInternalLost(): Unit = Unit

    internal suspend fun register() {
        logger.debug { "Registering ability $this for player ${abilityPlayer.name}." }
        this.activateEvents()
        this.handleInternalGained()
        this.handleAbilityGained()
    }

    internal suspend fun unregister() {
        logger.debug { "Unregistering ability $this for player ${abilityPlayer.name}." }
        this.handleInternalLost()
        this.handleAbilityLost()
        this.listener.unregisterListener()
        this.abilityJobs.clear { cancel(CancellationException("Ability has been unregistered.")) }
    }

    private fun activateEvents() {
        forwarders[this::class].forEach { func ->
            val selectorAnnotation = func.findAnnotation<OriginEventSelector>()!!
            val kClass = (func.extensionReceiverParameter?.type?.classifier ?: func.valueParameters[0].type.classifier).castOrThrow<KClass<Event>>()

            this.listener.event(
                kClass,
                getKoin().get<Terix>(),
                selectorAnnotation.priority,
                selectorAnnotation.ignoreCancelled,
                func.hasAnnotation<RunAsync>()
            ) {
                Option.fromNullable(selectorAnnotation.selector(this).swap().orNull())
                    .filter { player -> player.uniqueId == abilityPlayer.uniqueId }
                    .tap { func.callSuspend(this@Ability, this) }
            }
        }
    }

    /**
     * Subscribes to a flow that is later cancelled when the ability is unregistered.
     * This introduces a catch, which is captured and logged to Sentry.
     * This flow is launched in the [TickService] dispatcher.
     *
     * @receiver The flow to subscribe to.
     */
    protected fun Flow<*>.abilitySubscription() {
        abilityJobs += this.catch { err ->
            Sentry.captureException(err)
            Sentry.addBreadcrumb(
                Breadcrumb().apply {
                    message = "Error while handling tick in ability."
                    level = SentryLevel.ERROR
                    setData("ability", this@Ability.name)
                    setData("player", abilityPlayer.name)
                }
            )
        }.launchIn(plugin.scope + TickService.threadContext)
    }

    /** Trace and debug the ability */
    protected fun abilityBreadcrumb(message: String) {
        plugin.launchAsync {
            val breadcrumb = Breadcrumb()
            breadcrumb.type = "trace"
            breadcrumb.category = "origin.ability.$name:${abilityPlayer.name}"
            breadcrumb.level = SentryLevel.DEBUG
            breadcrumb.message = message

            breadcrumb.setData("player", "${abilityPlayer.name}:${abilityPlayer.uniqueId}")
            breadcrumb.setData("origin", linkedOrigin.name)
            breadcrumb.setData("ability", name)

            plugin.log.trace(scope = breadcrumb.category, msg = message)
            Sentry.addBreadcrumb(breadcrumb)
        }
    }

    public companion object {
        private val forwarders = Caffeine.newBuilder()
            .build<KClass<out Ability>, ImmutableSet<KFunction<*>>> { clazz ->
                clazz.declaredMembers.asSequence()
                    .filterIsInstance<KFunction<*>>()
                    .filter { it.hasAnnotation<OriginEventSelector>() }
                    .associateWith { it.findAnnotation<OriginEventSelector>()!! }
                    .filter { (_, annotation) -> annotation.selector.selector is EventSelector.PlayerSelector } // We forward by the player not by origin.
                    .filter { (func, annotation) -> annotation.selector.selector.isCompatible(func) }.keys.toImmutableSet()
            }
    }
}
