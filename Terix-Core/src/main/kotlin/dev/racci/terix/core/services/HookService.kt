package dev.racci.terix.core.services

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.pm
import dev.racci.minix.api.plugin.MinixLogger
import dev.racci.minix.api.utils.collections.CollectionUtils.clear
import dev.racci.terix.api.Terix
import me.angeschossen.lands.api.integration.LandsIntegration
import me.clip.placeholderapi.PlaceholderAPIPlugin
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.event.server.PluginEnableEvent
import org.bukkit.plugin.Plugin
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.time.Duration
import kotlin.reflect.KClass

typealias HookInvoker = () -> HookService.HookService

class HookService(override val plugin: Terix) : Extension<Terix>() {

    override val name = "Hook Service"

    private val loadedHooks by lazy { mutableMapOf<KClass<out Plugin>, HookService>() }
    private val unloadedHooks by lazy { mutableMapOf<KClass<out Plugin>, HookService>() }
    private val unregisteredHooks by lazy { mutableMapOf<KClass<out Plugin>, HookInvoker>() }

    inline operator fun <reified T : HookService> get(kClass: KClass<T> = T::class): T? = hooks[kClass] as? T

    val hooks: LoadingCache<KClass<out HookService>, HookService> = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(30))
        .build { kClass: KClass<out HookService> ->
            log.debug { "Looking for hook $kClass" }
            loadedHooks.values.find {
                log.debug { "Checking if ${it::class} is $kClass" }
                it::class == kClass
            }
        }

    @Suppress("UNCHECKED_CAST") // Because its the only way to get this class, Yes it is quite unsafe if the dev changes anything
    override suspend fun handleEnable() {
        unregisteredHooks.putAll(
            listOf(
                PlaceholderAPIPlugin::class to { PlaceholderAPIHook() },
                Class.forName("me.angeschossen.lands.Lands").kotlin as KClass<out Plugin> to { LandsHook() }
            )
        )

        pm.plugins.forEach {
            if (it.isEnabled) {
                val pluginKClass = it::class
                unloadedHooks.remove(pluginKClass)?.let { hook ->
                    hook.doSetup()
                    loadedHooks += pluginKClass to hook
                    hooks.put(hook::class, hook)
                }
            }
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

    private suspend fun loadHook(plugin: KClass<out Plugin>, hookService: HookService? = null, hookInvoker: HookInvoker? = null) {
        (hookService ?: hookInvoker?.invoke())?.let { hook ->
            hook.doSetup()
            loadedHooks += plugin to hook
            hooks.put(hook::class, hook)
        }
    }

    private suspend fun unloadHook(plugin: KClass<out Plugin>, hook: HookService) {
        hook.doUnload()
        unloadedHooks += plugin to hook
        hooks.invalidate(hook::class)
    }

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
                it.disable()
                integration = null
            }
        }
    }
}
