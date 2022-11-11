package dev.racci.terix.api.data

import arrow.analysis.pre
import arrow.optics.optics
import dev.racci.minix.api.extensions.server
import dev.racci.minix.api.utils.now
import kotlinx.datetime.Instant
import org.bukkit.block.BlockState
import org.bukkit.block.data.BlockData
import org.bukkit.metadata.FixedMetadataValue

@optics public data class TemporaryReplacement(
    public val metaValue: FixedMetadataValue,
    public val replacedState: BlockState,
    public val replacementData: BlockData,
) {
    public lateinit var committedAt: Instant; private set

    public fun commit(): TemporaryReplacement {
        pre(server.isPrimaryThread) { "TemporaryReplacement.commit() must be called on the main thread!" }

        committedAt = now()
        replacedState.block.blockData = replacementData
        replacedState.block.setMetadata(META_KEY, metaValue)

        return this
    }

    public fun beenMutated(): Boolean {
        pre(::committedAt.isInitialized) { "TemporaryReplacement.commit() must be called before beenMutated()!" }

        val activeState = replacedState.block.state
        return activeState.type != replacementData.material ||
            !activeState.hasMetadata(META_KEY) ||
            !activeState.getMetadata(META_KEY).contains(metaValue)
    }

    public fun undoCommit() {
        pre(server.isPrimaryThread) { "TemporaryReplacement.undoCommit() must be called on the main thread!" }
        pre(::committedAt.isInitialized) { "TemporaryReplacement.commit() must be called before undoCommit()!" }

        replacedState.block.blockData = replacedState.blockData
    }

    public companion object {
        private const val META_KEY = "replacement-owner"
    }
}
