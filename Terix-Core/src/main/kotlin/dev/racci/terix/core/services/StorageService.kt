package dev.racci.terix.core.services

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.racci.minix.api.extension.Extension
import dev.racci.terix.api.Terix
import dev.racci.terix.core.storage.User
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.properties.Delegates

class StorageService(override val plugin: Terix) : Extension<Terix>() {

    override val name = "Storage Service"
    override val dependencies = persistentListOf(OriginService::class)

    private var config by Delegates.notNull<HikariConfig>()
    private var dataSource by Delegates.notNull<HikariDataSource>()

    override suspend fun handleEnable() {
        setupDatabase()
    }

    private fun setupDatabase() {
        config = HikariConfig()
        config.jdbcUrl = "jdbc:sqlite:${plugin.dataFolder.path}/database.db"
        config.connectionTestQuery = "SELECT 1"
        config.addDataSourceProperty("cachePrepStmts", true)
        config.addDataSourceProperty("prepStmtCacheSize", "250")
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        dataSource = HikariDataSource(config)
        Database.connect(dataSource)
        log.info { "Connected to database." }

        transaction {
            SchemaUtils.createMissingTablesAndColumns(User)
        }
    }
}
