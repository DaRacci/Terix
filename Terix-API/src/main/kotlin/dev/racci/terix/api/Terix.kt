package dev.racci.terix.api

import dev.racci.minix.api.coroutine.launch
import dev.racci.minix.api.plugin.MinixPlugin
import kotlinx.coroutines.delay
import org.jetbrains.annotations.ApiStatus
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@ApiStatus.Internal
public abstract class Terix : MinixPlugin() {
    public inline fun whileEnabled(
        context: CoroutineContext = EmptyCoroutineContext,
        delay: Duration = 25.milliseconds,
        crossinline block: suspend () -> Unit
    ) {
        launch(context) {
            while (isEnabled) {
                block()
                delay(delay)
            }
        }
    }
}
