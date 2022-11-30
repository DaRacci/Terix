package dev.racci.terix.api.origins.abilities.passive

import arrow.core.getOrElse
import dev.racci.minix.api.events.player.PlayerMoveXYZEvent
import dev.racci.minix.api.utils.minecraft.rangeTo
import dev.racci.minix.api.utils.ticks
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.annotations.OriginEventSelector
import dev.racci.terix.api.data.placement.TemporaryPlacement
import dev.racci.terix.api.data.placement.tempPlacement
import dev.racci.terix.api.data.player.TerixPlayer
import dev.racci.terix.api.extensions.above
import dev.racci.terix.api.origins.abilities.RayCastingSupplier
import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.origin.Origin
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.shapes.CollisionContext
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.Levelled
import org.bukkit.craftbukkit.v1_19_R1.block.data.CraftBlockData
import kotlin.time.Duration

// TODO -> sounds and particle in center location
// TODO -> Better "circle" around player
public class FluidWalker(
    override val abilityPlayer: TerixPlayer,
    override val linkedOrigin: Origin,
    override val placementData: BlockData,
    override val placementDuration: Duration = 5.ticks,
    public val radius: Double = 3.0,
    public val replaceableMaterials: Set<Material>
) : PassiveAbility(),
    TemporaryPlacement.BlockDataProvider,
    TemporaryPlacement.DurationLimited {
    private val temporaryPlacement = tempPlacement(
        removablePredicate = { pos -> pos.above().distance(abilityPlayer.location) > radius }
    )

    @OriginEventSelector(EventSelector.PLAYER)
    public suspend fun PlayerMoveXYZEvent.handle() {
        val nmsWorld = player.world.toNMS()
        val blockState = (temporaryPlacement.replacementData as CraftBlockData).state
        val surfaceLocation = RayCastingSupplier.of(player)
            .map { trace -> trace.hitPosition.toLocation(player.world) }
            .getOrElse { return }
        val range = surfaceLocation.clone().add(-radius + 0.5, 0.0, -radius + 0.5).rangeTo(surfaceLocation.clone().add(radius, 0.0, radius))

        val locations = range
            .filter { pos -> pos.distance(surfaceLocation) <= radius }
            .filter { pos -> player.world.getBlockAt(pos.above()).state.type.isEmpty }
            .filter { pos ->
                val state = player.world.getBlockState(pos)
                val blockData = state.blockData
                state.type in replaceableMaterials && (blockData !is Levelled || blockData.level == 0)
            }.filter { pos -> pos.block.blockData == temporaryPlacement.replacementData || pos.block.canPlace(temporaryPlacement.replacementData) && nmsWorld.isUnobstructed(blockState, BlockPos(pos.x, pos.y, pos.z), CollisionContext.empty()) }

        sync { locations.forEach { temporaryPlacement.commit(it) } }
    }
}
