package dev.racci.terix.api.origins.abilities

import dev.racci.minix.api.destructors.component1
import dev.racci.minix.api.destructors.component2
import dev.racci.minix.api.destructors.component3
import dev.racci.minix.api.events.PlayerMoveXYZEvent
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.onlinePlayers
import dev.racci.minix.api.extensions.taskAsync
import dev.racci.minix.api.extensions.ticks
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.dsl.dslMutator
import dev.racci.terix.api.extensions.playSound
import dev.racci.terix.api.sentryBreadcrumb
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.potion.PotionEffectType
import java.util.UUID
import kotlin.time.Duration

// TODO -> Don't force elytra if the player isn't moving.
// TODO -> If the player is still only levitate up.
class Levitate : Ability(AbilityType.TOGGLE) {

    private val glideMap = HashSet<Player>()

    init {
        event<PlayerMoveXYZEvent>(EventPriority.MONITOR, true) {
            if (!this@Levitate.isActivated(this.player.uniqueId)) return@event
            if (this.from.z == this.to.z && this.from.x == this.to.x) return@event

            startGliding(player)
            player.velocity.add(player.location.direction.normalize().multiply(3))
        }

        event<EntityToggleGlideEvent>(EventPriority.LOWEST, true) {
            val player = entity as? Player ?: return@event
            if (glideMap.contains(player)) cancel()
        }

        val locTracker = HashMap<UUID, Pair<Double, Double>>()
        taskAsync(repeatDelay = 1.ticks) {
            if (onlinePlayers.isEmpty()) return@taskAsync
            if (glideMap.isEmpty()) return@taskAsync

            for (player in glideMap) {
                if (locTracker.putIfAbsent(player.uniqueId, player.location.y to player.location.y) == null) continue

                locTracker.computeIfPresent(player.uniqueId) { _, (lastX, lastZ) ->
                    val (newX, _, newZ) = player.location
                    val diffX = newX - lastX
                    val diffZ = newZ - lastZ

                    if (diffX == 0.0 || diffZ == 0.0) {
                        player.isGliding = false
                        glideMap -= player
                    }

                    newX to newZ
                }
            }
        }
    }

    override suspend fun onActivate(player: Player) {
        player.playSound(SOUND.first, SOUND.second, SOUND.third)

        sync {
            player.addPotionEffect(
                dslMutator<PotionEffectBuilder> {
                    type = PotionEffectType.LEVITATION
                    duration = Duration.INFINITE
                    ambient = true
                    key = NamespacedKey("terix", KEY)
                }.asNew().get()
            )
        }
    }

    override suspend fun onDeactivate(player: Player) {
        glideMap -= player
        val types = mutableListOf<PotionEffectType>()
        for (potion in player.activePotionEffects) {
            if (potion.type != PotionEffectType.LEVITATION || potion.key?.key != KEY) continue
            types += potion.type
            break
        }

        player.playSound(SOUND.first, SOUND.second, SOUND.third)
        sync { types.forEach(player::removePotionEffect) }
    }

    private fun startGliding(player: Player) {
        if (glideMap.contains(player)) return

        sentryBreadcrumb(SCOPE, "levitate.gliding.start")

        glideMap += player
        player.isGliding = true
    }

    companion object {
        const val KEY = "origin_ability_levitate"
        val SOUND = Triple("minecraft:entity.phantom.flap", 1f, 1f)
    }
}
