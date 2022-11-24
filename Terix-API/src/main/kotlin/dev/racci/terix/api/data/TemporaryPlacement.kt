package dev.racci.terix.api.data

import arrow.optics.optics
import dev.racci.minix.api.coroutine.launch
import dev.racci.minix.api.coroutine.minecraftDispatcher
import dev.racci.minix.api.coroutine.scope
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.scheduler
import dev.racci.minix.api.flow.eventFlow
import dev.racci.minix.api.utils.getKoin
import dev.racci.minix.api.utils.now
import dev.racci.minix.api.utils.ticks
import dev.racci.terix.api.Terix
import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.origins.abilities.Ability
import dev.racci.terix.api.origins.origin.Origin
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import kotlinx.datetime.Instant
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.metadata.FixedMetadataValue
import java.util.concurrent.DelayQueue
import java.util.concurrent.Delayed
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

@optics
public data class TemporaryPlacement(
    public val abilityRef: Ability,
    public val replacedState: BlockState,
    public val replacementData: BlockData,
    public val immutable: Boolean,
    public val removeAfter: Duration = Duration.INFINITE,
    public val initTemplate: Boolean = false,
    public val metaValue: FixedMetadataValue = FixedMetadataValue(getKoin().get<Terix>(), abilityRef.abilityPlayer.uniqueId.toString())
) : Delayed {
    public var committedAt: Instant? = null; private set

    public fun commit(): TemporaryPlacement {
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

    public fun copyFor(pos: Location): TemporaryPlacement {
        return copy(replacedState = pos.block.state)
    }

    public fun copyFor(block: Block): TemporaryPlacement {
        return copy(replacedState = block.state)
    }

    override fun getDelay(unit: TimeUnit): Long {
        val committedAt = committedAt ?: return Long.MAX_VALUE
        return unit.convert((committedAt + removeAfter).toEpochMilliseconds() - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
    }

    override fun compareTo(other: Delayed): Int = getDelay(TimeUnit.MILLISECONDS).compareTo(other.getDelay(TimeUnit.MILLISECONDS))

    init {
        if (!initTemplate && removeAfter != Duration.INFINITE) needingRemoval.add(this)
    }

    public companion object {
        private const val META_KEY = "replacement-owner"
        private val needingRemoval: DelayQueue<TemporaryPlacement> = DelayQueue()

        init {
            val terix = getKoin().get<Terix>()
            val mainScope = terix.scope + terix.minecraftDispatcher

            terix.eventFlow<BlockBreakEvent>(priority = EventPriority.LOWEST)
                .filter { event -> event.block.hasMetadata(META_KEY) }
                .filterNot { event -> event.block.getMetadata(META_KEY).all { meta -> meta.asString() == event.player.uniqueId.toString() } }
                .onEach(BlockBreakEvent::cancel)
                .launchIn(mainScope)

            terix.eventFlow<PluginDisableEvent>()
                .filter { event -> event.plugin === terix }
                .onEach { needingRemoval.toList().forEach(TemporaryPlacement::undoCommit) }
                .launchIn(mainScope)

            scheduler { runnable ->

                val currentOrigin = mutableMapOf<Player, Origin>()

                fun TemporaryPlacement.changedOrigin(): Boolean =
                    this.abilityRef.linkedOrigin !== currentOrigin.getOrPut(this.abilityRef.abilityPlayer) { TerixPlayer.cachedOrigin(this.abilityRef.abilityPlayer) }

                fun TemporaryPlacement.changedWorld(): Boolean =
                    this.replacedState.location.world != this.abilityRef.abilityPlayer.world

                fun TemporaryPlacement.maybeRadiusLimited(): Boolean =
                    this.abilityRef is RadiusLimited && this.replacedState.location.distance(this.abilityRef.abilityPlayer.location.toCenterLocation()) > this.abilityRef.placementRadius

                val removing = needingRemoval.filter { it.changedOrigin() || it.changedWorld() || it.maybeRadiusLimited() }

                if (removing.isEmpty()) return@scheduler
                runnable.plugin.launch(runnable.plugin.minecraftDispatcher) {
                    removing.forEach { entry ->
                        entry.undoCommit()
                        needingRemoval.remove(entry)
                    }
                }
            }.runAsyncTaskTimer(terix, 5.ticks, 5.ticks)
        }

        /** Generates a new template. */
        public operator fun getValue(
            thisRef: Ability,
            property: Any?
        ): TemporaryPlacement {
            require(thisRef is BlockDataProvider) { "Cannot use this property delegate on an ability that does not provide a block data." }

            return TemporaryPlacement(
                thisRef,
                thisRef.abilityPlayer.world.getBlockAt(0, 0, 0).state,
                thisRef.placementData,
                thisRef is Immutable,
                (thisRef as? DurationLimited)?.placementDuration ?: Duration.INFINITE,
                true
            )
        }
    }

    public interface RadiusLimited {
        public val placementRadius: Double
    }

    public interface Immutable

    public interface BlockDataProvider {
        public val placementData: BlockData
    }

    public interface DurationLimited {
        public val placementDuration: Duration
    }
}
