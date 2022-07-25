package dev.racci.terix.api

import dev.racci.minix.api.coroutine.asyncDispatcher
import dev.racci.minix.api.coroutine.minecraftDispatcher
import dev.racci.minix.api.extensions.server
import dev.racci.minix.api.utils.getKoin
import io.sentry.protocol.User
import kotlinx.coroutines.withContext
import org.bukkit.entity.Player
import java.util.UUID

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

fun origin(player: Player) = PlayerData.cachedOrigin(player)

private val sentryUsers = hashMapOf<UUID, User>()

fun Player.sentryUser() = sentryUsers.getOrPut(uniqueId) {
    val user = User()
    user.id = this.uniqueId.toString()
    user.username = this.name
    user.ipAddress = this.address.address.hostAddress
    user.others = mapOf(
        "minecraft_protocol" to this.protocolVersion.toString(),
        "minecraft_brand" to this.clientBrandName.orEmpty(),
        "terix_origin" to origin(this).name
    )

    user
}
