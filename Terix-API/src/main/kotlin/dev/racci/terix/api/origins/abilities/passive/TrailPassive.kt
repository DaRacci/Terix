package dev.racci.terix.api.origins.abilities.passive

import arrow.core.Some
import arrow.optics.copy
import dev.racci.minix.api.events.player.PlayerMoveFullXYZEvent
import dev.racci.minix.api.extensions.collections.clear
import dev.racci.minix.api.utils.now
import dev.racci.terix.api.annotations.OriginEventSelector
import dev.racci.terix.api.data.TemporaryReplacement
import dev.racci.terix.api.data.replacedState
import dev.racci.terix.api.origins.abilities.RayCastingSupplier
import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.services.TickService
import kotlinx.coroutines.flow.onEach
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.metadata.FixedMetadataValue
import kotlin.properties.Delegates
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public class TrailPassive(
    override val abilityPlayer: Player,
    override val linkedOrigin: Origin
) : PassiveAbility() {
    private val trailCache = linkedSetOf<TemporaryReplacement>()
    private val templateReplacement by lazy {
        TemporaryReplacement(
            FixedMetadataValue(plugin, abilityPlayer.uniqueId.toString()),
            abilityPlayer.location.block.state,
            material.createBlockData()
        )
    }

    public var material: Material by Delegates.notNull()
    public var trailLength: Int = 3
    public var trailDuration: Duration = 1.seconds

    override suspend fun handleAbilityGained() {
        TickService.filteredPlayer(abilityPlayer)
            .onEach { removeIfExpired(); newTailPart() }
            .abilitySubscription()
    }

    override suspend fun handleAbilityLost() {
        sync { trailCache.clear(TemporaryReplacement::undoCommit) }
    }

    @OriginEventSelector(EventSelector.PLAYER)
    public fun PlayerMoveFullXYZEvent.handle() {
        newTailPart().tap { removeIfLimit() }
    }

    @OriginEventSelector(EventSelector.ENTITY)
    public suspend fun PlayerChangedWorldEvent.handle() {
        trailCache.clear { undoCommit() }
    }

    private fun newTailPart() = RayCastingSupplier.of(abilityPlayer)
        .mapNotNull { trace -> trace.hitBlock }
        .filter { block -> block.isSolid }
        .map { block -> block.getRelative(BlockFace.UP) }
        .filter { block -> block.type.isEmpty }
        .tap { block ->
            sync {
                templateReplacement.copy {
                    TemporaryReplacement.replacedState set block.state
                }.commit().also(trailCache::add)
            }
        }

    private fun removeIfExpired() {
        if (trailCache.size <= 1) return

        Some(trailCache.first())
            .filter { layer -> now() > layer.committedAt + trailDuration }
            .tap { layer -> trailCache.remove(layer) }
            .filterNot { layer -> layer.beenMutated() }
            .tap { layer -> sync { layer.undoCommit() } }
    }

    private fun removeIfLimit() {
        if (trailCache.size <= trailLength) return

        Some(trailCache.first())
            .tap { layer -> trailCache.remove(layer) }
            .filterNot { layer -> layer.beenMutated() }
            .tap { layer -> sync { layer.undoCommit() } }
    }
}
