package dev.racci.terix.api.data

import dev.racci.minix.api.utils.now
import kotlinx.datetime.Instant
import java.util.concurrent.Delayed
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/**
 * A cooldown wrapping a [Duration] and a [Instant] to determine when the cooldown is over.
 *
 * @property start The start of the cooldown.
 * @property duration The duration of the cooldown.
 */
public data class Cooldown(
    public val start: Instant,
    public val duration: Duration
) : Delayed {
    /** @return The remaining duration until the cooldown expires. */
    public fun remaining(): Duration = duration - (now() - start)

    /** @return Whether the cooldown is over. */
    public fun expired(): Boolean = start == Instant.DISTANT_PAST || (start + duration) <= now()

    /** @return Whether the cooldown is not over. */
    public fun notExpired(): Boolean = !expired()

    public fun isNone(): Boolean = this === NONE

    override fun getDelay(unit: TimeUnit): Long = if (isNone()) {
        Long.MAX_VALUE
    } else unit.convert(remaining().toJavaDuration())

    override fun compareTo(other: Delayed): Int = getDelay(TimeUnit.MILLISECONDS).compareTo(other.getDelay(TimeUnit.MILLISECONDS))

    public companion object {
        /** An Empty Cooldown. */
        public val NONE: Cooldown = Cooldown(Instant.DISTANT_PAST, Duration.ZERO)

        /** @return A new cooldown with the given [duration] starting at the current time. */
        public fun of(duration: Duration): Cooldown = Cooldown(now(), duration)
    }
}
