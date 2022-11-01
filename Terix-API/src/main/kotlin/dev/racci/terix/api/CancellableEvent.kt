package dev.racci.terix.api

import dev.racci.minix.api.extensions.cancel
import org.bukkit.event.Cancellable

public fun interface CancellableEvent<T : Cancellable> {
    public fun T.shouldCancel(): Boolean

    public operator fun T.invoke() {
        if (shouldCancel()) cancel()
    }
}
