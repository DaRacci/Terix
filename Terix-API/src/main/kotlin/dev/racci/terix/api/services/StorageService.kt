package dev.racci.terix.api.services

import dev.racci.minix.api.services.StorageService
import dev.racci.minix.api.utils.getKoin
import dev.racci.terix.api.Terix
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Transaction

public interface StorageService : StorageService<Terix> {
    public companion object {
        public fun <T> transaction(
            statement: Transaction.() -> T
        ): T {
            var result: T? = null
            runBlocking {
                getKoin().get<dev.racci.terix.api.services.StorageService>().withDatabase {
                    result = statement()
                }
            }

            return result!!
        }
    }
}
