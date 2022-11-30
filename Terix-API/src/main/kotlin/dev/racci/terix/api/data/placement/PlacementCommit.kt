package dev.racci.terix.api.data.placement

import dev.racci.minix.api.utils.now
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.plus
import kotlinx.datetime.until
import org.bukkit.Location
import org.bukkit.block.BlockState
import java.util.concurrent.Delayed
import java.util.concurrent.TimeUnit

public data class PlacementCommit(
    val pos: Location,
    val placementRef: TemporaryPlacement
)

public data class FulfilledPlacementCommit(
    val placementRef: TemporaryPlacement,
    val capturedState: BlockState,
    val revertAt: Instant
) : Delayed {
    override fun compareTo(other: Delayed): Int = when (other) {
        is FulfilledPlacementCommit -> revertAt.compareTo(other.revertAt)
        else -> revertAt.compareTo(now().plus(other.getDelay(TimeUnit.MILLISECONDS), DateTimeUnit.MILLISECOND))
    }

    override fun getDelay(unit: TimeUnit): Long = now().until(revertAt, DateTimeUnit.NANOSECOND)
}
