package dev.racci.terix.api.origins.abilities.passives

import arrow.core.getOrElse
import dev.racci.minix.api.events.player.PlayerMoveFullXYZEvent
import dev.racci.minix.api.extensions.taskAsync
import dev.racci.minix.api.utils.minecraft.rangeTo
import dev.racci.minix.api.utils.ticks
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.extensions.above
import dev.racci.terix.api.origins.abilities.PassiveAbility
import dev.racci.terix.api.origins.origin.Origin
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.shapes.CollisionContext
import org.bukkit.Material
import org.bukkit.block.data.Levelled
import org.bukkit.craftbukkit.v1_19_R1.block.data.CraftBlockData
import org.bukkit.entity.Player
import kotlin.properties.Delegates

public class FluidWalker(
    player: Player,
    origin: Origin
) : PassiveAbility(player, origin) {
    public var radius: Double = 3.0
    public var fluidType: Material by Delegates.notNull()
    public var replacement: Material by Delegates.notNull()

    override suspend fun onActivate() {
        subscribe<PlayerMoveFullXYZEvent> {
            val nmsWorld = player.world.toNMS()

            val blockData = replacement.createBlockData()
            val blockState = (blockData as CraftBlockData).state
            val surfaceLocation = RayCastingSupplier.of(player)
                .map { trace -> trace.hitPosition.toLocation(player.world).toCenterLocation() }
                .getOrElse { return@subscribe }
            val range = surfaceLocation.clone().add(-radius, 0.0, -radius).rangeTo(surfaceLocation.clone().add(radius, 0.0, radius))

            val locations = range
                .filter { pos -> pos.distance(surfaceLocation) <= radius }
                .filter { pos -> player.world.getBlockAt(pos.above()).state.type.isEmpty }
                .filter { pos -> (player.world.getBlockState(pos).blockData as? Levelled)?.level == 0 }
                .filter { pos -> pos.block.canPlace(blockData) && nmsWorld.isUnobstructed(blockState, BlockPos(pos.x, pos.y, pos.z), CollisionContext.empty()) }

            sync {
                locations.forEach { pos ->
                    pos.block.type = replacement
                    taskAsync(5.ticks, 5.ticks) { runnable ->
                        if (pos.distance(player.location.toCenterLocation()) > radius) {
                            sync {
                                pos.block.breakNaturally()
                                pos.block.type = fluidType
                            }
                            runnable.cancel()
                        }
                    }
                }
            }
        }
    }
}
