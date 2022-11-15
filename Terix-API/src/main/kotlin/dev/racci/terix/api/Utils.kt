package dev.racci.terix.api

import dev.racci.minix.api.coroutine.launch
import dev.racci.minix.api.extensions.taskAsync
import dev.racci.minix.api.scheduler.CoroutineTask
import dev.racci.minix.api.utils.getKoin
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryLevel
import io.sentry.protocol.User
import kotlinx.coroutines.Dispatchers
import org.bukkit.entity.Player
import java.util.UUID
import kotlin.coroutines.CoroutineContext

private val sentryUsers = hashMapOf<UUID, User>()
private val terix by getKoin().inject<Terix>()
private const val SCOPE = "terix.sentry.scoped"

public fun Player.sentryUser(): User = sentryUsers.getOrPut(uniqueId) {
    val user = User()
    user.id = this.uniqueId.toString()
    user.username = this.name
    user.ipAddress = this.address.address.hostAddress
    user.data = mapOf(
        "minecraft_protocol" to this.protocolVersion.toString(),
        "minecraft_brand" to this.clientBrandName.orEmpty(),
        "terix_origin" to TerixPlayer.cachedOrigin(this).name
    )

    user
}

public fun sentryBreadcrumb(
    category: String,
    message: String? = null,
    type: String = "trace",
    level: SentryLevel = SentryLevel.DEBUG
): CoroutineTask = taskAsync(plugin = terix) {
    val breadcrumb = Breadcrumb()
    breadcrumb.type = type
    breadcrumb.category = category
    breadcrumb.level = level
    breadcrumb.message = message

    terix.log.debug(scope = category, msg = message)
    Sentry.addBreadcrumb(breadcrumb)
}

public suspend fun sentryScoped(
    player: Player,
    category: String,
    message: String? = null,
    type: String = "trace",
    level: SentryLevel = SentryLevel.DEBUG,
    context: CoroutineContext = Dispatchers.Unconfined,
    block: suspend () -> Unit
) {
    terix.launch(context) {
        Sentry.pushScope()
        Sentry.setUser(player.sentryUser())
        terix.log.trace(scope = SCOPE) { "Entered scope for player ${player.name}" }
        sentryBreadcrumb(category, message, type, level)

        try {
            block()
        } catch (e: Exception) {
            Sentry.captureException(e)
            throw e
        } finally {
            terix.log.trace(scope = SCOPE) { "Exited scope for player ${player.name}" }
            Sentry.popScope()
        }
    }
}
