package dev.racci.terix.api

import dev.racci.minix.api.extensions.taskAsync
import dev.racci.minix.api.utils.getKoin
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryLevel
import io.sentry.protocol.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.bukkit.entity.Player
import java.util.UUID
import kotlin.coroutines.CoroutineContext

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

fun sentryBreadcrumb(
    player: Player,
    category: String,
    message: String,
    type: String = "trace",
    level: SentryLevel = SentryLevel.DEBUG
) = taskAsync(plugin = getKoin().get<Terix>()) {
    Sentry.pushScope()
    Sentry.setUser(player.sentryUser())

    val breadcrumb = Breadcrumb()
    breadcrumb.type = type
    breadcrumb.category = category
    breadcrumb.level = level
    breadcrumb.message = message

    Sentry.addBreadcrumb(breadcrumb)
    Sentry.popScope()
}

suspend fun <R> sentryScoped(
    player: Player,
    action: String,
    context: CoroutineContext = Dispatchers.Unconfined,
    block: suspend () -> R
) = CoroutineScope(context).async {
    Sentry.pushScope()
    Sentry.setUser(player.sentryUser())

    val breadcrumb = Breadcrumb()
    breadcrumb.type = "trace"
    breadcrumb.category = "${player.name}.$action"
    breadcrumb.level = SentryLevel.DEBUG

    Sentry.addBreadcrumb(breadcrumb)

    return@async try {
        block()
    } catch (e: Exception) {
        Sentry.captureException(e)
        null
    } finally {
        Sentry.popScope()
    }
}
