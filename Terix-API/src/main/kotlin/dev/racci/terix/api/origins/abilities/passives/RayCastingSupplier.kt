package dev.racci.terix.api.origins.abilities.passives

import arrow.core.Option
import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import com.github.benmanes.caffeine.cache.Caffeine
import org.bukkit.FluidCollisionMode
import org.bukkit.entity.Player
import org.bukkit.util.RayTraceResult
import org.bukkit.util.Vector
import java.util.concurrent.TimeUnit

public object RayCastingSupplier {
    private const val RAY_DIST = 3.0

    private val rayCastCache: AsyncLoadingCache<Player, RayTraceResult?> = Caffeine.newBuilder()
        .maximumSize(100)
        .expireAfterWrite(50, TimeUnit.MILLISECONDS)
        .buildAsync<Player, RayTraceResult?> { player -> player.world.rayTraceBlocks(player.location, Vector(0.0, -90.0, 0.0), RAY_DIST, FluidCollisionMode.ALWAYS, true) }

    public fun of(
        player: Player,
    ): Option<RayTraceResult> = Option.fromNullable(rayCastCache.get(player).get())
}
