package me.racci.terix.listeners

import com.github.shynixn.mccoroutine.asyncDispatcher
import com.github.shynixn.mccoroutine.minecraftDispatcher
import kotlinx.coroutines.withContext
import me.racci.raccicore.events.PlayerMoveFullXYZEvent
import me.racci.raccicore.utils.extensions.KotlinListener
import me.racci.terix.Terix
import me.racci.terix.Terix.Companion.removeMetadata
import me.racci.terix.Terix.Companion.setMetadata
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.data.Levelled
import org.bukkit.event.EventHandler
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.NumberConversions
import org.bukkit.util.Vector
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

class PlayerMoveListener : KotlinListener {

    @EventHandler
    suspend fun onLavaWalk(event: PlayerMoveFullXYZEvent) = withContext(Terix.instance.asyncDispatcher) {
        val player = event.player
        val circle = VectorUtils.getCircle(3)
        for (vector in circle) {
            val loc = player.location.add(vector).add(0.0, -1.0, 0.0)
            val block = player.world.getBlockAt(loc)
            if (block.type != Material.LAVA) {
                continue
            }
            val data = block.blockData as Levelled
            if (data.level != 0) {
                continue
            }
            withContext(Terix.instance.minecraftDispatcher) {
                block.type = Material.CRYING_OBSIDIAN
                setMetadata(block, "byLavaWalker")

                val replace = object : BukkitRunnable() {
                    override fun run() {
                        if (block.type == Material.CRYING_OBSIDIAN && player.world
                            .getBlockAt(player.location.add(0.0, -1.0, 0.0)) != block
                        ) {
                            block.type = Material.LAVA
                            removeMetadata(block, "byLavaWalker")
                            this.cancel()
                        }
                    }
                }
                Terix.run({
                }, 2, true)
                Bukkit.getScheduler().runTaskLater(
                    Terix.instance,
                    Runnable {
                        if (block.type == Material.CRYING_OBSIDIAN) {
                            if (player.world.getBlockAt(player.location.add(0.0, -1.0, 0.0)) != block) {
                                block.type = Material.LAVA
                                block.removeMetadata("byLavaWalker", Terix.instance)
                            } else {
                                replace.runTaskTimer(Terix.instance, 120, 120)
                            }
                        }
                    },
                    120
                )
            }
        }
    }
}

object VectorUtils {
    /**
     * If vector has all components as finite.
     *
     * @param vector The vector to check.
     * @return If the vector is finite.
     */
    fun isFinite(vector: Vector): Boolean {
        try {
            NumberConversions.checkFinite(vector.x, "x not finite")
            NumberConversions.checkFinite(vector.y, "y not finite")
            NumberConversions.checkFinite(vector.z, "z not finite")
        } catch (e: IllegalArgumentException) {
            return false
        }
        return true
    }

    /**
     * Only keep largest part of normalised vector.
     * For example: -0.8, 0.01, -0.2 would become -1, 0, 0.
     *
     * @param vec The vector to simplify.
     * @return The vector, simplified.
     */
    fun simplifyVector(vec: Vector): Vector {
        val x: Double = abs(vec.x)
        val y: Double = abs(vec.y)
        val z: Double = abs(vec.z)
        var max = x.coerceAtLeast(y.coerceAtLeast(z))
        if (x > 1 || z > 1) {
            max = y
        }
        return if (max == x) {
            if (vec.x < 0) {
                Vector(-1, 0, 0)
            } else Vector(1, 0, 0)
        } else if (max == y) {
            if (vec.y < 0) {
                Vector(0, -1, 0)
            } else Vector(0, 1, 0)
        } else {
            if (vec.z < 0) {
                Vector(0, 0, -1)
            } else Vector(0, 0, 1)
        }
    }

    /**
     * Get circle as relative vectors.
     *
     * @param radius The radius.
     * @return An array of [Vector]s.
     */
    fun getCircle(radius: Int): Array<Vector> {
        val cached: Array<Vector>? = CIRCLE_CACHE[radius]
        if (cached != null) {
            return cached
        }
        val vectors: MutableList<Vector> = ArrayList()
        var xoffset = -radius.toDouble()
        var zoffset = -radius.toDouble()
        while (zoffset <= radius) {
            while (xoffset <= radius) {
                if (sqrt(xoffset * xoffset + zoffset * zoffset).roundToInt() <= radius) {
                    vectors.add(Vector(xoffset, 0.0, zoffset))
                } else {
                    xoffset++
                    continue
                }
                xoffset++
            }
            xoffset = -radius.toDouble()
            zoffset++
        }
        val result: Array<Vector> = vectors.toTypedArray()
        CIRCLE_CACHE[radius] = result
        return result
    }

    /**
     * Get square as relative vectors.
     *
     * @param radius The radius of the square.
     * @return An array of [Vector]s.
     */
    fun getSquare(radius: Int): Array<Vector> {
        val vectors: MutableList<Vector> = ArrayList()
        var xoffset = -radius
        var zoffset = -radius
        while (zoffset <= radius) {
            while (xoffset <= radius) {
                vectors.add(Vector(xoffset, 0, zoffset))
                xoffset++
            }
            xoffset = -radius
            zoffset++
        }
        return vectors.toTypedArray()
    }

    /**
     * Get cube as relative vectors.
     *
     * @param radius The radius of the cube.
     * @return An array of [Vector]s.
     */
    fun getCube(radius: Int): Array<Vector> {
        val vectors: MutableList<Vector> = ArrayList()
        for (y in -radius..radius) {
            for (z in -radius..radius) {
                for (x in -radius..radius) {
                    vectors.add(Vector(x, y, z))
                }
            }
        }
        return vectors.toTypedArray()
    }

    /**
     * Cached circles to prevent many sqrt calls.
     */
    private val CIRCLE_CACHE: MutableMap<Int, Array<Vector>> = HashMap()
}
