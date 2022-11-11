package dev.racci.terix.api.origins.abilities.passives

import arrow.core.Some
import arrow.optics.copy
import dev.racci.minix.api.coroutine.scope
import dev.racci.minix.api.events.player.PlayerMoveFullXYZEvent
import dev.racci.minix.api.extensions.collections.clear
import dev.racci.minix.api.utils.now
import dev.racci.terix.api.annotations.OriginEventSelector
import dev.racci.terix.api.data.TemporaryReplacement
import dev.racci.terix.api.data.replacedState
import dev.racci.terix.api.origins.abilities.PassiveAbility
import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.services.TickService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.metadata.FixedMetadataValue
import kotlin.properties.Delegates
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public class TrailPassive(
    player: Player,
    origin: Origin
) : PassiveAbility(player, origin) {
    private val trailCache = linkedSetOf<TemporaryReplacement>()
    private val templateReplacement by lazy {
        TemporaryReplacement(
            FixedMetadataValue(plugin, abilityPlayer.uniqueId.toString()),
            player.location.block.state,
            material.createBlockData(),
        )
    }
    private lateinit var job: Job

    public var material: Material by Delegates.notNull()
    public var trailLength: Int = 3
    public var trailDuration: Duration = 1.seconds

    override suspend fun onActivate() {
        job = TickService.playerFlow
            .filter { it.uniqueId == abilityPlayer.uniqueId }
            .onEach { removeIfExpired(); newTailPart() }
            .launchIn(plugin.scope + TickService.threadContext)
    }

    override suspend fun onDeactivate() {
        job.cancel()
        sync { trailCache.clear { undoCommit() } }
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
