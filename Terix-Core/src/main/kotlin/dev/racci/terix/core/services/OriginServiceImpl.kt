package dev.racci.terix.core.services

import arrow.core.Either
import arrow.core.getOrHandle
import arrow.core.toOption
import com.github.benmanes.caffeine.cache.Caffeine
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.annotations.MinixDsl
import dev.racci.minix.api.annotations.RunAsync
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.onlinePlayers
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.minix.api.services.DataService
import dev.racci.minix.api.utils.collections.multiMapOf
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.Terix
import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.annotations.OriginEventSelector
import dev.racci.terix.api.data.TerixConfig
import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.enums.EventSelector.TargetSelector.Companion.isCompatible
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.core.extensions.sanitise
import dev.racci.terix.core.origins.AethenOrigin
import dev.racci.terix.core.origins.AxolotlOrigin
import dev.racci.terix.core.origins.BeeOrigin
import dev.racci.terix.core.origins.BlizzOrigin
import dev.racci.terix.core.origins.DragonOrigin
import dev.racci.terix.core.origins.FairyOrigin
import dev.racci.terix.core.origins.HumanOrigin
import dev.racci.terix.core.origins.MerlingOrigin
import dev.racci.terix.core.origins.NetherbornOrigin
import dev.racci.terix.core.origins.SlimeOrigin
import dev.racci.terix.core.origins.VampireOrigin
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentMap
import org.bukkit.event.Event
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.valueParameters
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

@MappedExtension(Terix::class, "Origin Service", [], OriginService::class)
public class OriginServiceImpl(override val plugin: Terix) : OriginService, Extension<Terix>() {
    private val modifierCache = Caffeine.newBuilder()
        .expireAfterAccess(1.minutes.toJavaDuration())
        .build<KClass<*>, Any> { kClass -> kClass.primaryConstructor!!.call(this) }

    private val origins = mutableMapOf<KClass<out Origin>, Origin>()
    private var dirtyCache: List<String>? = null
    private var dirtyRegistry: PersistentMap<String, Origin>? = null

    public val registry: PersistentMap<String, Origin>
        get() = dirtyRegistry ?: origins.values.associateBy { it.name.lowercase() }.toPersistentMap().also { dirtyRegistry = it }
    public val registeredOrigins: List<String>
        get() = dirtyCache ?: registry.keys.map {
            it.replaceFirstChar(Char::titlecaseChar)
        }.toList().also { dirtyCache = it }
    override val defaultOrigin: Origin get() = registry.getOrElse(getKoin().get<DataService>().get<TerixConfig>().defaultOrigin) { origins[HumanOrigin::class]!! }

    override suspend fun handleEnable() {
        populateRegistry()

        origins.values.forEach(::activateEvents)
    }

    override suspend fun handleDisable() {
        origins.values.forEach { it.handleUnload() }
        onlinePlayers.forEach { player -> player.sanitise() }
    }

    private suspend fun populateRegistry() {
        registry {
            add<AethenOrigin>()
            add<AxolotlOrigin>()
            add<BeeOrigin>()
            add<BlizzOrigin>()
            add<DragonOrigin>()
            add<FairyOrigin>()
            add<HumanOrigin>()
            add<MerlingOrigin>()
            add<NetherbornOrigin>()
            add<SlimeOrigin>()
            add<VampireOrigin>()
        }
    }

    override fun getOrigins(): PersistentMap<KClass<out Origin>, Origin> = origins.toPersistentMap()

    override fun getOrigin(origin: KClass<out Origin>): Origin = origins[origin] ?: throw NoSuchElementException("Origin ${origin.simpleName} not found")

    public inline fun <reified T : Origin> getOrigin(): Origin = getOrigin(T::class)

    override fun getOrigin(name: String): Origin = registry[name.lowercase()] ?: throw NoSuchElementException("No origin registered for $name")

    override fun getOriginOrNull(name: String?): Origin? {
        if (name.isNullOrBlank()) return null
        return registry[name.lowercase()]
    }

    @MinixDsl
    public suspend fun registry(block: suspend RegistryModifier.() -> Unit) { block(modifierCache.get(RegistryModifier::class).castOrThrow()) }

    private val eventMap = multiMapOf<Origin, Pair<KFunction<*>, suspend (Event, EventSelector) -> Unit>>()
    private fun registerForwarders(origin: Origin) {
        origin::class.declaredMembers.asSequence()
            .filterIsInstance<KFunction<*>>()
            .filter { it.returnType.classifier == Unit::class }
            .filter { func -> func.hasAnnotation<OriginEventSelector>() }
            .filter { func -> func.findAnnotation<OriginEventSelector>()!!.selector.selector.isCompatible(func) }
            .forEach { func ->
                eventMap.put(
                    origin,
                    Pair(func) { event, annotation ->
                        annotation(event).fold(
                            ifLeft = { player -> TerixPlayer.cachedOrigin(player) },
                            ifRight = { selectedOrigin -> selectedOrigin }
                        ).toOption()
                            .filter { selectedOrigin -> selectedOrigin === origin }
                            .map { selectedOrigin -> func.callSuspend(selectedOrigin, event) }
                    }
                )
            }
    }

    // TODO -> Either create my own more efficient event system or idk do something.
    private fun activateEvents(origin: Origin) {
        eventMap[origin]?.forEach { (func, handler) ->
            val selectorAnnotation = func.findAnnotation<OriginEventSelector>()!!
            val kClass = (func.extensionReceiverParameter?.type?.classifier ?: func.valueParameters[0].type.classifier).castOrThrow<KClass<Event>>()

            this.eventListener.event(
                type = kClass,
                plugin = plugin,
                priority = selectorAnnotation.priority,
                ignoreCancelled = selectorAnnotation.ignoreCancelled,
                forceAsync = func.hasAnnotation<RunAsync>(),
                block = { handler(this, selectorAnnotation.selector) }
            )
        }
    }

    public inner class RegistryModifier {

        @MinixDsl
        public suspend fun add(originBuilder: suspend (Terix) -> Origin) {
            val origin = Either.catch { originBuilder(plugin) }
                .getOrHandle { err -> return plugin.log.error(err) { "Exception thrown while instancing origin." } }

            runCatching { origin.handleRegister() }
                .onFailure { err -> return plugin.log.error(err) { "Exception thrown while registering origin." } }

            origin.builderCache.asMap().values.forEach { builder -> builder.insertInto(origin) }
            origin.builderCache.invalidateAll() // TODO -> Don't invalidate when origins become per player as this will be reused to create new origins.

            origins.putIfAbsent(origin::class, origin)
            registerForwarders(origin)
        }

        @MinixDsl
        public suspend inline fun <reified T : Origin> add(kClazz: KClass<T> = T::class) {
            add {
                logger.debug { "Creating origin ${kClazz.simpleName}" }
                kClazz.primaryConstructor!!.call(plugin)
            }
        }
    }

    public companion object : ExtensionCompanion<OriginServiceImpl>()
}
