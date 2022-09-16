package dev.racci.terix.core.services

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.event
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.Terix
import dev.racci.terix.api.TerixPlayer
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.properties.Delegates

@MappedExtension(Terix::class, "Storage Service", [OriginService::class])
class StorageService(override val plugin: Terix) : Extension<Terix>() {

    private var dataSource by Delegates.notNull<HikariDataSource>()

    override suspend fun handleLoad() {
        val config = HikariConfig().apply {
            this.jdbcUrl = "jdbc:sqlite:${plugin.dataFolder.path}/database.db"
            this.connectionTestQuery = "SELECT 1"
            this.addDataSourceProperty("cachePrepStmts", true)
            this.addDataSourceProperty("prepStmtCacheSize", "250")
            this.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }

        dataSource = HikariDataSource(config)
    }

    override suspend fun handleEnable() {
        getKoin().setProperty(Terix.DATABASE, Database.connect(dataSource))

        transaction(Terix.database) {
            SchemaUtils.createMissingTablesAndColumns(TerixPlayer.User)
        }
        event<AsyncPlayerPreLoginEvent> {
            transaction(Terix.database) { TerixPlayer.findById(this@event.uniqueId) ?: TerixPlayer.new(this@event.uniqueId) {} }
        }
    }

    override suspend fun handleUnload() {
        getKoin().deleteProperty(Terix.DATABASE)
        dataSource.close()
    }
}
