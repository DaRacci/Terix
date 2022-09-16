package dev.racci.terix.api

import dev.racci.minix.api.plugin.MinixPlugin
import dev.racci.minix.api.utils.getKoin
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.exposed.sql.Database

@ApiStatus.Internal
abstract class Terix : MinixPlugin() {

    companion object {
        const val DATABASE = "terix:database"

        val database get() = getKoin().getProperty<Database>(DATABASE)
    }
}
