package dev.racci.terix.api

import dev.racci.minix.api.coroutine.launch
import dev.racci.minix.api.extensions.message
import dev.racci.minix.api.extensions.taskAsync
import dev.racci.minix.api.scheduler.CoroutineTask
import dev.racci.minix.api.utils.getKoin
import dev.racci.minix.api.utils.now
import dev.racci.terix.api.data.Lang
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryLevel
import io.sentry.protocol.User
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.kyori.adventure.extra.kotlin.text
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
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
            getKoin().get<Lang>().generic.error[
                "message" to {
                    text {
                        append(Component.text("There was an error while within your player scope;"))
                        val prefix = Component.text("» ").color(NamedTextColor.WHITE)
                        val timeStamp = now().toLocalDateTime(TimeZone.currentSystemDefault())
                        append(Component.newline())
                        append(prefix)
                        append(Component.text("please report this to a developer with the timestamp of;"))
                        append(Component.newline())
                        append(prefix)
                        append(Component.text("${timeStamp.monthNumber}-${timeStamp.dayOfMonth} ${timeStamp.hour}:${timeStamp.minute}:${timeStamp.second}"))
                        append(Component.newline())
                        colorIfAbsent(NamedTextColor.RED)

                        append(prefix)
                        append(Component.text("[${e::class.simpleName} - ${e.message}] occurred at;").color(NamedTextColor.YELLOW))
                        append(Component.newline())
                        append(prefix)
                        val element = e.stackTrace.first { it.className.startsWith("dev.racci.terix") }
                        Component.text(element.className.removePrefix("dev.racci.terix."))
                            .append(Component.text("."))
                            .append(Component.text(element.methodName))
                            .color(NamedTextColor.GRAY).also(::append)

                        Component.text(":")
                            .append(Component.text(element.lineNumber))
                            .color(NamedTextColor.DARK_GRAY).also(::append)
                    }
                }
            ] message player
            throw e
        } finally {
            terix.log.trace(scope = SCOPE) { "Exited scope for player ${player.name}" }
            Sentry.popScope()
        }
    }
}
