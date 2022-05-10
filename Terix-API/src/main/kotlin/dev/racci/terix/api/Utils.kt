package dev.racci.terix.api

import dev.racci.minix.api.coroutine.minecraftDispatcher
import dev.racci.minix.api.extensions.server
import dev.racci.minix.api.utils.getKoin
import kotlinx.coroutines.withContext

@PublishedApi
internal val mainThread by lazy { getKoin().get<Terix>().minecraftDispatcher }

suspend inline fun ensureMainThread(crossinline block: () -> Unit) {
    if (server.isPrimaryThread) {
        block()
    } else {
        withContext(mainThread) {
            block()
        }
    }
}
