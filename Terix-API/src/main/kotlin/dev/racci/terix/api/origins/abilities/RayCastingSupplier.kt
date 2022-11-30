package dev.racci.terix.api.origins.abilities

import arrow.core.Option
import com.google.common.collect.MinMaxPriorityQueue.maximumSize
import com.sksamuel.aedile.core.LoadingCache
import com.sksamuel.aedile.core.caffeineBuilder
import dev.racci.terix.api.extensions.downVector
import kotlinx.coroutines.Dispatchers
import org.bukkit.FluidCollisionMode
import org.bukkit.entity.Player
import org.bukkit.util.RayTraceResult
import kotlin.time.Duration.Companion.milliseconds

public object RayCastingSupplier {
    private const val RAY_DIST = 9.0

    private val rayCastCache: LoadingCache<Player, RayTraceResult?> = caffeineBuilder<Player, RayTraceResult?> {
        maximumSize = 100
        expireAfterWrite = 15.milliseconds
        dispatcher = Dispatchers.Default
    }.build { player -> player.world.rayTraceBlocks(player.location, downVector(), RAY_DIST, FluidCollisionMode.ALWAYS, true) }

    public suspend fun of(
        player: Player
    ): Option<RayTraceResult> = Option.fromNullable(rayCastCache.get(player))
}
