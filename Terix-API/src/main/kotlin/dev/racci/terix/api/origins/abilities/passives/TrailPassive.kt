package dev.racci.terix.api.origins.abilities.passives

import arrow.core.Some
import dev.racci.minix.api.coroutine.scope
import dev.racci.minix.api.events.player.PlayerMoveFullXYZEvent
import dev.racci.minix.api.extensions.collections.clear
import dev.racci.minix.api.utils.now
import dev.racci.terix.api.annotations.OriginEventSelector
import dev.racci.terix.api.origins.abilities.PassiveAbility
import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.services.TickService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import kotlinx.datetime.Instant
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.BlockState
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
    private val fixedMetadata = FixedMetadataValue(plugin, abilityPlayer.uniqueId.toString())
    private val blockMeta: String by lazy { "${origin.name}-trail-${material.name}" }
    private val trailCache = linkedSetOf<TrailPart>()
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
        sync { trailCache.clear { resetLayer() } }
    }

    @OriginEventSelector(EventSelector.PLAYER)
    public fun PlayerMoveFullXYZEvent.handle() {
        newTailPart().tap { removeIfLimit() }
    }

    @OriginEventSelector(EventSelector.ENTITY)
    public suspend fun PlayerChangedWorldEvent.handle() {
        trailCache.clear { resetLayer() }
    }

    private fun newTailPart() = RayCastingSupplier.of(abilityPlayer)
        .mapNotNull { trace -> trace.hitBlock }
        .filter { block -> block.isSolid }
        .map { block -> block.getRelative(BlockFace.UP) }
        .filter { block -> block.type.isEmpty }
        .tap { block -> sync { addLayer(block) } }

    private fun removeIfExpired() {
        if (trailCache.size <= 1) return

        Some(trailCache.first())
            .filter { layer -> now() > layer.placed + trailDuration }
            .tap { layer -> trailCache.remove(layer) }
            .filterNot { layer -> layer.mutated() }
            .tap { layer -> sync { layer.resetLayer() } }
    }

    private fun removeIfLimit() {
        if (trailCache.size <= trailLength) return

        Some(trailCache.first())
            .tap { layer -> trailCache.remove(layer) }
            .filterNot { layer -> layer.mutated() }
            .tap { layer -> sync { layer.resetLayer() } }
    }

    private fun addLayer(block: Block): TrailPart {
        val layer = TrailPart(block.location, this, block.state)
        layer.setLayer()
        trailCache.add(layer)
        return layer
    }

    public data class TrailPart(
        val pos: Location,
        val ref: TrailPassive,
        val beforeState: BlockState,
        val placed: Instant = now(),
    ) {
        public fun setLayer() {
            pos.block.type = ref.material
            pos.block.setMetadata(ref.blockMeta, ref.fixedMetadata)
        }

        public fun mutated(): Boolean {
            val state = pos.block.state
            return state.type != ref.material || !state.hasMetadata(ref.blockMeta) || !state.getMetadata(ref.blockMeta).contains(ref.fixedMetadata)
        }

        public fun resetLayer() {
            pos.block.blockData = beforeState.blockData
        }
    }
}
