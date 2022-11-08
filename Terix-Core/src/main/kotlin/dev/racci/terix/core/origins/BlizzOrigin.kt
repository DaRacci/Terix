package dev.racci.terix.core.origins

import arrow.core.toOption
import com.github.benmanes.caffeine.cache.Caffeine
import dev.racci.minix.api.events.player.PlayerMoveFullXYZEvent
import dev.racci.minix.api.extensions.taskAsync
import dev.racci.minix.api.utils.collections.multiMapOf
import dev.racci.minix.api.utils.getKoin
import dev.racci.minix.api.utils.minecraft.rangeTo
import dev.racci.minix.api.utils.now
import dev.racci.minix.api.utils.ticks
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.Terix
import dev.racci.terix.api.annotations.OriginEventSelector
import dev.racci.terix.api.dsl.FoodPropertyBuilder
import dev.racci.terix.api.dsl.dslMutator
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.sounds.SoundEffect
import kotlinx.datetime.Instant
import net.kyori.adventure.text.format.TextColor
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.shapes.CollisionContext
import org.bukkit.FluidCollisionMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Levelled
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.util.Vector
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

// TODO -> Walk on powdered snow.
// TODO -> Cake!
public class BlizzOrigin(override val plugin: Terix) : Origin() {
    private val snowLocation = multiMapOf<Player, SnowLayer>()

    override val name: String = "Blizz"
    override val colour: TextColor = TextColor.fromHexString("#7ac2ff")!!

    override suspend fun handleRegister() {
        sounds.hurtSound = SoundEffect("entity.panda.bite")
        sounds.deathSound = SoundEffect("entity.squid.death")
        sounds.ambientSound = SoundEffect("entity.skeleton_horse.ambient")

        damage {
            listOf(
                DamageCause.FIRE,
                DamageCause.LAVA,
                DamageCause.FIRE_TICK,
                DamageCause.HOT_FLOOR
            ) *= 2.5

            DamageCause.FREEZE /= 3
        }

        food {
            Material.SNOWBALL += dslMutator<FoodPropertyBuilder> {
                canAlwaysEat = true
                nutrition = 3
                saturationModifier = 3f
                fastFood = true
            }
            listOf(Material.TROPICAL_FISH, Material.PUFFERFISH, Material.SALMON, Material.COD) *= 3
            listOf(Material.COOKED_SALMON, Material.COOKED_COD) /= 3
        }

        item {
            material = Material.POWDER_SNOW_BUCKET
            lore = """
                <gold>A magical snowball that will 
                <green>freeze</green> any player that
                <red>touches it
            """.trimIndent()
        }
    }

    override suspend fun handleLoad(player: Player) {
        player.freezeTicks = 0
        player.lockFreezeTicks(true)
    }

    override suspend fun handleChangeOrigin(event: PlayerOriginChangeEvent) {
        event.player.lockFreezeTicks(false)
    }

    @OriginEventSelector(EventSelector.PLAYER)
    public fun EntityDamageByEntityEvent.handle() {
        entity.freezeTicks += 20
    }

    @OriginEventSelector(EventSelector.PLAYER)
    public fun PlayerMoveFullXYZEvent.handle() {
        frostWalker(player)
        newSnowLayer(player).tap { removeIfLimit(player) }
    }

    override suspend fun onTick(player: Player) {
        removeIfExpired(player)
        newSnowLayer(player) // Ensures that the player always has a snow layer even if still.
    }

    private fun newSnowLayer(player: Player) = player.world
        .rayTraceBlocks(player.location, downVector(), RAY_DIST, FluidCollisionMode.NEVER).toOption()
        .mapNotNull { trace -> trace.hitBlock }
        .filter { block -> block.isSolid }
        .map { block -> block.getRelative(BlockFace.UP) }
        .filter { block -> block.type.isEmpty }
        .tap { block -> sync { addLayer(player, block) } }

    private fun removeIfExpired(player: Player) {
        if ((snowLocation[player]?.size ?: 0) <= 1) return

        snowLocation[player]?.firstOrNull().toOption()
            .filter { (_, placed) -> now() > placed + SNOW_DURATION }
            .tap { layer -> snowLocation.remove(player, layer) }
            .filterNot { layer -> layer.mutated(player) }
            .tap { layer -> sync { layer.resetLayer() } }
    }

    private fun removeIfLimit(player: Player) {
        if ((snowLocation[player]?.size ?: 0) <= MAX_LAYERS) return

        snowLocation[player]!!.first().toOption()
            .tap { layer -> snowLocation.remove(player, layer) }
            .filterNot { layer -> layer.mutated(player) }
            .tap { layer -> sync { layer.resetLayer() } }
    }

    private fun addLayer(
        player: Player,
        block: Block,
    ): SnowLayer {
        val layer = SnowLayer(block.location)
        layer.setLayer(player)
        snowLocation.put(player, layer)
        return layer
    }

    private fun frostWalker(player: Player) {
        val nmsWorld = player.world.toNMS()
        val blockData = Blocks.FROSTED_ICE.defaultBlockState()
        val surfaceTrace = player.world.rayTraceBlocks(player.location, downVector(), RAY_DIST, FluidCollisionMode.ALWAYS, true)
        val surfaceLocation = surfaceTrace?.hitPosition?.toLocation(player.world)?.toCenterLocation() ?: return
        val range = surfaceLocation.clone().add(-ICE_RANGE, 0.0, -ICE_RANGE).rangeTo(surfaceLocation.clone().add(ICE_RANGE, 0.0, ICE_RANGE))

        val state = Material.FROSTED_ICE.createBlockData()
        val locations = range
            .filter { pos -> pos.distance(surfaceLocation) <= ICE_RANGE }
            .filter { pos -> player.world.getBlockAt(pos.above()).state.type.isEmpty }
            .filter { pos -> (player.world.getBlockState(pos).blockData as? Levelled)?.level == 0 }
            .filter { pos -> pos.block.canPlace(state) && nmsWorld.isUnobstructed(blockData, BlockPos(pos.x, pos.y, pos.z), CollisionContext.empty()) }

        sync {
            locations.forEach { pos ->
                pos.block.type = Material.FROSTED_ICE
                taskAsync(5.ticks, 5.ticks) { runnable ->
                    if (pos.distance(player.location.toCenterLocation()) > ICE_RANGE) {
                        sync {
                            pos.block.breakNaturally()
                            pos.block.type = Material.WATER
                        }
                        runnable.cancel()
                    }
                }
            }
        }
    }

    public data class SnowLayer(
        val pos: Location,
        override val placed: Instant = now(),
    ) : TemporaryLayer {
        public override fun setLayer(player: Player) {
            pos.block.type = Material.SNOW
            pos.block.setMetadata(BLOCK_META, valueCache[player])
        }

        public override fun resetLayer() {
            pos.block.breakNaturally()
        }

        public override fun mutated(player: Player): Boolean {
            val state = pos.block.state
            return state.type != Material.SNOW || !state.hasMetadata(BLOCK_META) || !state.getMetadata(BLOCK_META).contains(valueCache[player])
        }

        public companion object {
            private val terix by getKoin().inject<Terix>()
            private val valueCache = Caffeine.newBuilder()
                .expireAfterAccess(30, TimeUnit.SECONDS)
                .build<Player, FixedMetadataValue> { player -> FixedMetadataValue(terix, player.uniqueId.toString()) }
        }
    }

    private interface TemporaryLayer {
        val placed: Instant

        fun setLayer(player: Player)

        fun resetLayer()

        fun mutated(player: Player): Boolean
    }

    private companion object {
        const val RAY_DIST = 3.0
        const val BLOCK_META = "blizz_snow_layer"
        const val MAX_LAYERS = 3
        const val ICE_RANGE = 3.0

        val SNOW_DURATION = 1.seconds

        fun downVector() = Vector(0.0, -90.0, 0.0)

        fun Location.above(height: Double = 1.0) = this.clone().add(0.0, height, 0.0)
    }
}
