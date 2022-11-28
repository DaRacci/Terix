package dev.racci.terix.core.services

import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.event
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.Terix
import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.services.StorageService
import org.bukkit.event.EventPriority
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.jetbrains.exposed.sql.Table

@MappedExtension(Terix::class, "Storage Service", [OriginService::class], StorageService::class)
public class StorageServiceImpl(override val plugin: Terix) : StorageService, Extension<Terix>() {
    override val managedTable: Table by lazy { TerixPlayer.TerixPlayerEntity.table } // Lazy to prevent exception on init

    override suspend fun handleEnable() {
        event<AsyncPlayerPreLoginEvent>(EventPriority.MONITOR, true) {
            withDatabase { TerixPlayer.TerixPlayerEntity.findById(uniqueId) ?: TerixPlayer.TerixPlayerEntity.new(uniqueId) {} }
        }
    }
}
