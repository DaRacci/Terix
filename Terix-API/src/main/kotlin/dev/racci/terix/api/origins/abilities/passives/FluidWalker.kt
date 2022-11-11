package dev.racci.terix.api.origins.abilities.passives

import arrow.core.getOrElse
import dev.racci.minix.api.coroutine.launch
import dev.racci.minix.api.events.player.PlayerMoveFullXYZEvent
import dev.racci.minix.api.extensions.collections.clear
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.events
import dev.racci.minix.api.extensions.scheduler
import dev.racci.minix.api.utils.getKoin
import dev.racci.minix.api.utils.minecraft.rangeTo
import dev.racci.minix.api.utils.ticks
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.Terix
import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.annotations.OriginEventSelector
import dev.racci.terix.api.extensions.above
import dev.racci.terix.api.origins.abilities.PassiveAbility
import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.origin.Origin
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.shapes.CollisionContext
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.data.Levelled
import org.bukkit.craftbukkit.v1_19_R1.block.data.CraftBlockData
import org.bukkit.entity.Player
import org.bukkit.event.server.PluginDisableEvent
import java.util.concurrent.DelayQueue
import java.util.concurrent.Delayed
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

// TODO -> sounds and particle in center location
public class FluidWalker(
    player: Player,
    origin: Origin
) : PassiveAbility(player, origin) {
    public var radius: Double = 3.0
    public var fluidType: Material by Delegates.notNull()
    public var replacement: Material by Delegates.notNull()

    @OriginEventSelector(EventSelector.PLAYER)
    public fun PlayerMoveFullXYZEvent.handle() {
        val nmsWorld = player.world.toNMS()
        val blockData = replacement.createBlockData()
        val blockState = (blockData as CraftBlockData).state
        val surfaceLocation = RayCastingSupplier.of(player)
            .map { trace -> trace.hitPosition.toLocation(player.world).toCenterLocation() }
            .getOrElse { return }
        val range = surfaceLocation.clone().add(-radius, 0.0, -radius).rangeTo(surfaceLocation.clone().add(radius, 0.0, radius))

        val locations = range
            .filter { pos -> pos.distance(surfaceLocation) <= radius }
            .filter { pos -> player.world.getBlockAt(pos.above()).state.type.isEmpty }
            .filter { pos ->
                val state = player.world.getBlockState(pos)
                state.type == fluidType && (state.blockData as? Levelled)?.level == 0
            }
            .filter { pos -> pos.block.canPlace(blockData) && nmsWorld.isUnobstructed(blockState, BlockPos(pos.x, pos.y, pos.z), CollisionContext.empty()) }

        sync {
            locations.forEach { pos ->
                pos.block.type = replacement
                needingRemoval.add(RemovalEntry(pos, this@FluidWalker, 5.ticks.inWholeMilliseconds))
            }
        }
    }

    private data class RemovalEntry(
        val pos: Location,
        val ref: FluidWalker,
        val time: Long
    ) : Delayed {
        override fun getDelay(unit: TimeUnit): Long = unit.convert(time - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
        override fun compareTo(other: Delayed): Int = getDelay(TimeUnit.MILLISECONDS).compareTo(other.getDelay(TimeUnit.MILLISECONDS))
    }

    public companion object {
        private val needingRemoval: DelayQueue<RemovalEntry> = DelayQueue()

        init {
            getKoin().get<Terix>().events {
                this.event<PluginDisableEvent> {
                    if (this.plugin.name != "Terix") return@event
                    needingRemoval.clear { pos.block.type = ref.fluidType }
                }
            }

            scheduler { runnable ->
                val removing = arrayListOf<RemovalEntry>()
                val currentOrigin = mutableMapOf<Player, Origin>()
                for (entry in needingRemoval) {
                    when {
                        entry.ref.linkedOrigin !== currentOrigin.getOrPut(entry.ref.abilityPlayer) { TerixPlayer.cachedOrigin(entry.ref.abilityPlayer) } ||
                            entry.pos.world != entry.ref.abilityPlayer.world ||
                            entry.pos.distance(entry.ref.abilityPlayer.location.toCenterLocation()) > entry.ref.radius -> removing.add(entry)
                    }
                }

                if (removing.isEmpty()) return@scheduler
                runnable.plugin.launch {
                    removing.forEach { entry ->
                        entry.pos.block.type = entry.ref.fluidType
                        needingRemoval.remove(entry)
                    }
                }
            }.runAsyncTaskTimer(getKoin().get<Terix>(), 5.ticks, 5.ticks)
        }
    }
}
