package dev.racci.terix.core.services

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.utils.kotlin.ifInitialized
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.PlayerData
import dev.racci.terix.api.Terix
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

@MappedExtension(Terix::class, "Storage Service", [OriginService::class])
class StorageService(override val plugin: Terix) : Extension<Terix>() {

    private var config = lazy {
        val config = HikariConfig()
        config.jdbcUrl = "jdbc:sqlite:${plugin.dataFolder.path}/database.db"
        config.connectionTestQuery = "SELECT 1"
        config.addDataSourceProperty("cachePrepStmts", true)
        config.addDataSourceProperty("prepStmtCacheSize", "250")
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        config
    }
    private var dataSource = lazy { HikariDataSource(config.value) }

    override suspend fun handleEnable() {
        val database = Database.connect(dataSource.value)
        getKoin().setProperty("terix:database", database)

        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(PlayerData.User)
        }
        event<AsyncPlayerPreLoginEvent> {
            transaction(database) { PlayerData.findById(this@event.uniqueId) ?: PlayerData.new(this@event.uniqueId) {} }
        }
    }

    override suspend fun handleUnload() {
        dataSource.ifInitialized(HikariDataSource::close)
    }
}
