package dev.racci.terix.core.services

import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.ProtocolManager
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.pm
import dev.racci.minix.api.plugin.MinixLogger
import dev.racci.minix.api.utils.collections.CollectionUtils.clear
import dev.racci.minix.api.utils.collections.CollectionUtils.getCast
import dev.racci.terix.api.Terix
import dev.racci.terix.core.enchantments.SunResistance
import me.angeschossen.lands.api.integration.LandsIntegration
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.event.server.PluginEnableEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

typealias HookInvoker = () -> HookService.HookService

@MappedExtension(Terix::class, "Hook Service")
class HookService(override val plugin: Terix) : Extension<Terix>() {

    val protocolManager: ProtocolManager by lazy(ProtocolLibrary::getProtocolManager)
    val loadedHooks by lazy { mutableMapOf<KClass<out Plugin>, HookService>() }
    private val unloadedHooks by lazy { mutableMapOf<KClass<out Plugin>, HookService>() }
    private val unregisteredHooks by lazy { mutableMapOf<KClass<out Plugin>, HookInvoker>() }

    inline operator fun <reified T : HookService> get(kClass: KClass<T> = T::class): T? = loadedHooks.getCast(kClass)

    override suspend fun handleEnable() {
        listOfNotNull(
            hookPair<LandsHook>("me.angeschossen.lands.Lands"),
            hookPair<PlaceholderAPIHook>("me.clip.placeholderapi.PlaceholderAPIPlugin"),
            hookPair<EcoEnchantsHook>("com.willfp.ecoenchants.EcoEnchantsPlugin")
        ).let(unregisteredHooks::putAll)

        for (plugin in pm.plugins) {
            if (!plugin.isEnabled) continue

            val pluginKClass = plugin::class
            loadHook(pluginKClass, unloadedHooks.remove(pluginKClass), unregisteredHooks.remove(pluginKClass))
        }

        event<PluginEnableEvent> {
            val pluginKClass = plugin::class
            loadHook(pluginKClass, unloadedHooks.remove(pluginKClass), unregisteredHooks.remove(pluginKClass))
        }

        event<PluginDisableEvent> {
            val pluginKClass = plugin::class
            loadedHooks.remove(pluginKClass)?.let { hook ->
                unloadHook(pluginKClass, hook)
            }
        }
    }

    override suspend fun handleUnload() {
        loadedHooks.clear { plugin, hook ->
            unloadHook(plugin, hook)
        }
    }

    private suspend fun loadHook(
        plugin: KClass<out Plugin>,
        hookService: HookService? = null,
        hookInvoker: HookInvoker? = null
    ) {
        (hookService ?: hookInvoker?.invoke())?.let { hook ->
            hook.doSetup()
            loadedHooks += plugin to hook
        }
    }

    private suspend fun unloadHook(plugin: KClass<out Plugin>, hook: HookService) {
        hook.doUnload()
        unloadedHooks += plugin to hook
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified H : HookService> hookPair(className: String): Pair<KClass<out Plugin>, HookInvoker>? =
        try {
            Class.forName(className).kotlin as KClass<out Plugin> to { H::class.createInstance() }
        } catch (e: ClassNotFoundException) {
            null
        } catch (e: ClassCastException) { log.error(e) { "Failed to cast class $className to Plugin" }; null }

    interface HookService : KoinComponent {

        val plugin: Terix get() = get()
        val log: MinixLogger get() = plugin.log

        suspend fun doSetup() {}

        suspend fun doUnload() {}
    }

    class PlaceholderAPIHook : HookService, PlaceholderExpansion() {

        override fun persist() = true
        override fun canRegister() = true

        override fun getAuthor() = get<Terix>().description.authors.joinToString(", ")
        override fun getVersion() = get<Terix>().description.version
        override fun getIdentifier() = get<Terix>().description.name

        override suspend fun doSetup() {
            log.info { "Registering PlaceholderAPI Hook" }
            register()
        }
        override suspend fun doUnload() {
            log.info { "Unregistering PlaceholderAPI Hook" }
            unregister()
        }
    }

    class LandsHook : HookService {

        var integration: LandsIntegration? = null ; private set

        override suspend fun doSetup() {
            log.info { "Registering Lands Hook" }
            integration = LandsIntegration(plugin)
        }
        override suspend fun doUnload() {
            integration?.let {
                log.info { "Unregistering Lands Hook" }
                integration = null
            }
        }
    }

    class EcoEnchantsHook : HookService {

        val sunResistance: SunResistance? get() = SunResistance.instance

        override suspend fun doSetup() {
            log.info { "Registering EcoEnchants Hook" }
        }
    }

    companion object : ExtensionCompanion<dev.racci.terix.core.services.HookService>()
}
