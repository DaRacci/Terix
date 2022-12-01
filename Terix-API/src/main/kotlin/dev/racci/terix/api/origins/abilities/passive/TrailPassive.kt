package dev.racci.terix.api.origins.abilities.passive

import dev.racci.minix.api.events.player.PlayerMoveFullXYZEvent
import dev.racci.terix.api.annotations.OriginEventSelector
import dev.racci.terix.api.data.placement.TemporaryPlacement
import dev.racci.terix.api.data.placement.tempPlacement
import dev.racci.terix.api.data.player.TerixPlayer
import dev.racci.terix.api.origins.abilities.RayCastingSupplier
import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.services.TickService
import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public class TrailPassive(
    override val abilityPlayer: TerixPlayer,
    override val linkedOrigin: Origin,
    override val placementData: BlockData,
    override val placementDuration: Duration = 1.seconds,
    public var trailLength: Int = 3
) : PassiveAbility(),
    TemporaryPlacement.BlockDataProvider,
    TemporaryPlacement.Immutable,
    TemporaryPlacement.DurationLimited {
    private var trailSize: AtomicInt = atomic(0)
    private val templateReplacement = tempPlacement(
        removablePredicate = { _ -> trailSize.value > 1 },
        expeditedPredicate = { _ -> trailSize.value > trailLength },
        commitCallback = { trailSize.incrementAndGet() },
        removalCallback = { trailSize.decrementAndGet() }
    )

    override suspend fun handleAbilityGained() {
        TickService.filteredPlayer(abilityPlayer)
            .takeWhile { trailSize.value <= trailLength }
            .onEach { newTailPart() }
            .abilitySubscription()
    }

    @OriginEventSelector(EventSelector.PLAYER)
    public suspend fun PlayerMoveFullXYZEvent.handle() {
        newTailPart()
    }

    private suspend fun newTailPart() = RayCastingSupplier.of(abilityPlayer)
        .mapNotNull { trace -> trace.hitBlock }
        .filter { block -> block.isSolid }
        .map { block -> block.getRelative(BlockFace.UP) }
        .filter { block -> block.type.isEmpty }
        .tap { block -> templateReplacement.commit(block.location) }

//    private fun removeIfExpired() {
//        if (trailCache.size <= 1) return
//
//        Some(trailCache.removeFirst())
//            .filter { layer -> layer.removalTimer }
//            .filter { layer -> now() > layer.committedAt!! + trailDuration }
//            .filterNot { layer -> layer.beenMutated() }
//            .tap { layer -> sync { layer.undoCommit() } }
//    }

//    private fun removeIfLimit() {
//        if (trailCache.size <= trailLength) return
//
//        Some(trailCache.first())
//            .tap { layer -> trailCache.remove(layer) }
//            .filterNot { layer -> layer.beenMutated() }
//            .tap { layer -> sync { layer.undoCommit() } }
//    }
}
