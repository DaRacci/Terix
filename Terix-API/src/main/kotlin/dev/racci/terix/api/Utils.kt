package dev.racci.terix.api

import dev.racci.minix.api.coroutine.asyncDispatcher
import dev.racci.minix.api.coroutine.minecraftDispatcher
import dev.racci.minix.api.extensions.server
import dev.racci.minix.api.utils.getKoin
import kotlinx.coroutines.withContext

@PublishedApi internal val mainThread by lazy { getKoin().get<Terix>().minecraftDispatcher }
@PublishedApi internal val asyncThread by lazy { getKoin().get<Terix>().asyncDispatcher }

suspend inline fun ensureMainThread(crossinline block: suspend () -> Unit) {
    if (server.isPrimaryThread) {
        block()
    } else {
        withContext(mainThread) {
            block()
        }
    }
}

suspend inline fun ensureAsync(crossinline block: suspend () -> Unit) {
    if (!server.isPrimaryThread) {
        block()
    } else {
        withContext(asyncThread) {
            block()
        }
    }
}
