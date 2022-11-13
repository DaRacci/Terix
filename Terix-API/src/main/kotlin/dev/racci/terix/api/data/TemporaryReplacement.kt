package dev.racci.terix.api.data

import arrow.optics.optics
import dev.racci.minix.api.utils.now
import kotlinx.datetime.Instant
import org.bukkit.block.BlockState
import org.bukkit.block.data.BlockData
import org.bukkit.metadata.FixedMetadataValue

@optics
public data class TemporaryReplacement(
    public val metaValue: FixedMetadataValue,
    public val replacedState: BlockState,
    public val replacementData: BlockData
) {
    public var committedAt: Instant? = null; private set

    public fun commit(): TemporaryReplacement {
        committedAt = now()
        replacedState.block.blockData = replacementData
        replacedState.block.setMetadata(META_KEY, metaValue)

        return this
    }

    public fun beenMutated(): Boolean {
        if (committedAt == null) return false

        val activeState = replacedState.block.state
        return activeState.type != replacementData.material ||
            !activeState.hasMetadata(META_KEY) ||
            !activeState.getMetadata(META_KEY).contains(metaValue)
    }

    public fun undoCommit() {
        requireNotNull(committedAt) { "Cannot undo a commit that has not been committed." }

        replacedState.block.blockData = replacedState.blockData
        committedAt = null
    }

    public companion object {
        private const val META_KEY = "replacement-owner"
    }
}
