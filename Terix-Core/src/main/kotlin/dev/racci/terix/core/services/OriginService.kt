package dev.racci.terix.core.services

import dev.racci.minix.api.annotations.MinixDsl
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.utils.collections.ObservableAction
import dev.racci.minix.api.utils.collections.ObservableMap
import dev.racci.minix.api.utils.collections.observableMapOf
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.core.origins.HumanOrigin
import dev.racci.terix.core.origins.SlimeOrigin
import kotlinx.collections.immutable.persistentListOf
import kotlin.collections.set

class OriginService(override val plugin: Terix) : Extension<Terix>() {

    override val name = "Origin Service"
    override val dependencies = persistentListOf(StorageService::class)

    operator fun get(origin: String, ignoreCase: Boolean = false) = registry[if (ignoreCase) origin.lowercase() else origin]

    private val registryModifier = RegistryModifier()
    private val registry: ObservableMap<String, AbstractOrigin> = observableMapOf()

    val registeredOrigins by lazy { registry.keys.map { it.replaceFirstChar { c -> c.titlecase() } }.toMutableList() }
    val defaultOrigin: AbstractOrigin get() = this["human"]!!

    override suspend fun handleEnable() {
        startRegistry()
        populateRegistry()
    }

    // TODO Fix up players when an origin is removed from the registry
    private fun startRegistry() {
        registry.observe(ObservableAction.ADD, ObservableAction.REMOVE, ObservableAction.CLEAR) { origin, action ->
            val originName = origin.first.replaceFirstChar { c -> c.titlecase() }
            when (action) {
                ObservableAction.ADD -> {
                    registeredOrigins += originName
                    log.debug { "Registered origin: $originName" }
                }
                ObservableAction.REMOVE -> {
                    registeredOrigins -= originName
                    log.debug { "Unregistered origin: $originName" }
                }
                ObservableAction.CLEAR -> {
                    registeredOrigins.clear()
                    log.debug { "Cleared origin registry" }
                }
                else -> log.trace { "Unknown action for origin observer: $action" }
            }
        }
    }

    private suspend fun populateRegistry() {
        registry {
            add(::HumanOrigin)
            add(::SlimeOrigin)
        }
    }

    @MinixDsl
    suspend fun registry(block: suspend RegistryModifier.() -> Unit) { registryModifier.block() }

    inner class RegistryModifier {

        @MinixDsl
        suspend fun add(originBuilder: suspend (Terix) -> AbstractOrigin) {
            val origin = originBuilder(plugin)
            origin.onRegister()
            registry[origin.name.lowercase()] = origin
        }
        suspend fun remove(origin: String) = registry.remove(origin)
        suspend fun reload(origin: String) = registry.remove(origin).also { add(::HumanOrigin) }
    }
}