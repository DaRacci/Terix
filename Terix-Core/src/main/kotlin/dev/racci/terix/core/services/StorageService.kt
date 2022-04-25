package dev.racci.terix.core.services

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.utils.kotlin.ifInitialized
import dev.racci.terix.api.Terix
import dev.racci.terix.core.data.User
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
        log.info { "Connected to database." }
        Database.connect(dataSource.value)
        transaction {
            SchemaUtils.createMissingTablesAndColumns(User)
        }
    }

    override suspend fun handleUnload() {
        dataSource.ifInitialized(HikariDataSource::close)
    }
}
