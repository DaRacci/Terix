package dev.racci.terix.core.origins.abilities.keybind

import com.destroystokyo.paper.block.TargetBlockInfo
import dev.racci.minix.api.utils.minecraft.rangeTo
import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.data.TemporaryPlacement
import dev.racci.terix.api.extensions.above
import dev.racci.terix.api.origins.abilities.keybind.TriggeringKeybindAbility
import dev.racci.terix.api.origins.origin.Origin
import org.bukkit.Location
import org.bukkit.block.data.BlockData
import org.bukkit.util.Vector
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// TODO -> Triggering Charge that builds the wall over time.
public class WallErector(
    override val abilityPlayer: TerixPlayer,
    override val linkedOrigin: Origin,
    override val cooldownDuration: Duration = 1.minutes,
    override val placementDuration: Duration = 5.seconds,
    override val placementData: BlockData
) : TriggeringKeybindAbility(),
    TemporaryPlacement.Immutable,
    TemporaryPlacement.BlockDataProvider,
    TemporaryPlacement.DurationLimited {
    private val temporaryPlacement by TemporaryPlacement

//    override suspend fun handleTrigger() {
//        val multi = when {
//            false -> abilityPlayer.location.direction.clone().multiply(2.0).setY(0) // FIXME: Vector based angle
//            abilityPlayer.location.yaw in 35f..145f -> Vector(0.0, 0.0, 2.0)
//            else -> Vector(2.0, 0.0, 0.0)
//        }
//        val zeroedInitVec = initBlock.location.toCenterLocation().toVector().setY(0.0)
//        val zeroedPlayerVec = abilityPlayer.location.toCenterLocation().toVector().setY(0.0)
//        val distanceRequired = zeroedInitVec.distance(zeroedPlayerVec)
//    }

    // TODO -> Chunked building, Chunked removal.
    override suspend fun handleTrigger() {
        val targetBlock = abilityPlayer.getTargetBlock(5, TargetBlockInfo.FluidMode.NEVER)
        if (targetBlock == null || !targetBlock.isSolid) return failActivation()
        val initBlock = targetBlock.above()
        if (initBlock.type.isSolid || !initBlock.canPlace(temporaryPlacement.replacementData)) return failActivation()

        val isX = abilityPlayer.location.yaw.absoluteValue in 45.51f..134.49f // TODO -> Allow for more dynamic placement directions.
        val modifierVec = if (isX) Vector(0.0, 0.0, 2.0) else Vector(2.0, 0.0, 0.0)
        val firstPos = initBlock.location.subtract(modifierVec)
        val secondPos = initBlock.location.add(modifierVec).above(3.0)

        val filteredPositions = firstPos.rangeTo(secondPos)
            .map(Location::getBlock)
            .filter { block -> block.isEmpty && block.canPlace(temporaryPlacement.replacementData) }

        sync { filteredPositions.forEach { temporaryPlacement.copyFor(it).commit() } }
    }
}
