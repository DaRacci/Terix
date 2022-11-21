package dev.racci.terix.api.data

import dev.racci.minix.api.utils.now
import kotlinx.datetime.Instant
import kotlin.time.Duration

/**
 * A cooldown wrapping a [Duration] and a [Instant] to determine when the cooldown is over.
 *
 * @property start The start of the cooldown.
 * @property duration The duration of the cooldown.
 */
public data class Cooldown(
    public val start: Instant,
    public val duration: Duration
) {
    /** @return The remaining duration until the cooldown expires. */
    public fun remaining(): Duration = duration - (now() - start)

    /** @return Whether the cooldown is over. */
    public fun expired(): Boolean = (start + duration) <= now()

    /** @return Whether the cooldown is not over. */
    public fun notExpired(): Boolean = !expired()

    public companion object {
        /** An Empty Cooldown. */
        public val NONE: Cooldown = Cooldown(Instant.DISTANT_PAST, Duration.ZERO)

        /** @return A new cooldown with the given [duration] starting at the current time. */
        public fun of(duration: Duration): Cooldown = Cooldown(now(), duration)
    }
}
