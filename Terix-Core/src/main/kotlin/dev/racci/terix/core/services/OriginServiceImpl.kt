package dev.racci.terix.core.services

import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.annotations.MinixDsl
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.services.DataService
import dev.racci.minix.api.utils.collections.CollectionUtils.cacheOf
import dev.racci.minix.api.utils.unsafeCast
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.abilities.Ability
import dev.racci.terix.api.origins.abilities.Levitate
import dev.racci.terix.api.origins.abilities.Teleport
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.core.data.Config
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
import java.time.Duration
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.primaryConstructor

@MappedExtension(Terix::class, "Origin Service", bindToKClass = OriginService::class)
class OriginServiceImpl(override val plugin: Terix) : OriginService, Extension<Terix>() {
    private val modifierCache = cacheOf<KClass<*>, Any>({ constructors.first().call(this@OriginServiceImpl) }, { expireAfterAccess(Duration.ofMinutes(1)) })
    private val origins = mutableMapOf<KClass<out Origin>, Origin>()
    private var dirtyCache: Array<String>? = null
    private var dirtyRegistry: PersistentMap<String, Origin>? = null

    val abilities = mutableMapOf<KClass<out Ability>, Ability>()
    val registry: PersistentMap<String, Origin>
        get() = dirtyRegistry ?: origins.values.associateBy { it.name.lowercase() }.toPersistentMap().also { dirtyRegistry = it }
    val registeredOrigins: Array<String>
        get() = dirtyCache ?: registry.keys.map {
            it.replaceFirstChar(Char::titlecaseChar)
        }.toTypedArray().also { dirtyCache = it }
    val defaultOrigin: Origin get() = registry.getOrElse(getKoin().get<DataService>().get<Config>().defaultOrigin) { origins[HumanOrigin::class]!! }

    override suspend fun handleEnable() {
        populateAbilities()
        populateRegistry()
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

    private suspend fun populateAbilities() {
        abilities {
            add<Levitate>()
            add<Teleport>()
        }
    }

    override fun getAbilities(): PersistentMap<KClass<out Ability>, Ability> = abilities.toPersistentMap()

    override fun getOrigins(): PersistentMap<KClass<out Origin>, Origin> = origins.toPersistentMap()

    override fun getOrigin(origin: KClass<out Origin>): Origin = origins[origin] ?: error("Origin ${origin.simpleName} not found")

    inline fun <reified T : Origin> getOrigin() = getOrigin(T::class)

    override fun getOriginOrNull(origin: KClass<out Origin>): Origin? = origins[origin]

    override fun getAbility(ability: KClass<out Ability>): Ability = abilities[ability] ?: error("No ability registered for ${ability.simpleName}")

    override fun getAbilityOrNull(ability: KClass<out Ability>): Ability? = abilities[ability]

    override fun getOrigin(name: String): Origin = registry[name] ?: error("No origin registered for $name")

    override fun getOriginOrNull(name: String?): Origin? {
        if (name.isNullOrBlank()) return null
        return registry[name]
    }

    @MinixDsl
    suspend fun registry(block: suspend RegistryModifier.() -> Unit) { block(modifierCache.get(RegistryModifier::class).unsafeCast()) }

    @MinixDsl
    suspend fun abilities(block: suspend AbilityModifier.() -> Unit) {
        block(modifierCache.get(AbilityModifier::class).unsafeCast())
    }

    inner class RegistryModifier {

        @MinixDsl
        suspend fun add(originBuilder: suspend (Terix) -> Origin) {
            val origin = originBuilder(plugin)
            origin.onRegister()
            origins.putIfAbsent(origin::class, origin)
        }

        @MinixDsl
        suspend inline fun <reified T : Origin> add(kClazz: KClass<T> = T::class) {
            add { kClazz.primaryConstructor!!.call(plugin) }
        }
    }

    inner class AbilityModifier {

        @MinixDsl
        suspend fun add(abilityBuilder: suspend () -> Ability) {
            val inst = abilityBuilder()
            abilities[inst::class] = inst
        }

        @MinixDsl
        suspend inline fun <reified T : Ability> add(kClass: KClass<T> = T::class) {
            add(kClass::createInstance)
        }
    }

    companion object : ExtensionCompanion<OriginServiceImpl>()
}
