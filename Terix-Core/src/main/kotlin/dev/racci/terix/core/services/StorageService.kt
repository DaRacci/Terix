package dev.racci.terix.core.services

import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.services.StorageService
import dev.racci.minix.api.utils.getKoin
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.Terix
import dev.racci.terix.api.TerixPlayer
import kotlinx.coroutines.runBlocking
import org.bukkit.event.player.PlayerJoinEvent
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction

@MappedExtension(Terix::class, "Storage Service", [OriginService::class])
public class StorageService(override val plugin: Terix) : StorageService<Terix>, Extension<Terix>() {
    override val managedTable: Table by lazy { TerixPlayer.table } // Lazy to prevent exception on init

    override suspend fun handleEnable() {
        event<PlayerJoinEvent> {
            withDatabase {
                val terixPlayer = TerixPlayer.findById(player.uniqueId) ?: TerixPlayer.new(player.uniqueId) {}
                terixPlayer.load(TerixPlayer::origin)
            }
        }
    }

    public companion object {
        public fun <T> transaction(
            statement: Transaction.() -> T
        ): T {
            var result: T? = null
            runBlocking {
                getKoin().get<dev.racci.terix.core.services.StorageService>().withDatabase {
                    result = statement()
                }
            }

            return result!!
        }
    }
}
