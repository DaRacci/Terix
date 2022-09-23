package dev.racci.terix.api.origins.abilities

import dev.racci.minix.api.extensions.SimpleKListener
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.event
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.extensions.playSound
import dev.racci.terix.api.sentryBreadcrumb
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.potion.PotionEffectType
import java.util.UUID

class Levitate : Ability(AbilityType.TOGGLE) {

    private val glideMap = HashSet<UUID>()

    init {
        val listener = SimpleKListener(plugin)
        listener.event<EntityToggleGlideEvent>(
            EventPriority.LOWEST,
            true
        ) {
            val player = entity as? Player ?: return@event
            if (glideMap.contains(player.uniqueId)) cancel()
        }
    }

    override suspend fun onActivate(player: Player) {
        startGliding(player)
        player.velocity.add(player.location.direction.multiply(3))
        player.playSound(SOUND.first, SOUND.second, SOUND.third)

        sync {
            player.addPotionEffect(
                PotionEffectBuilder.build {
                    type = PotionEffectType.LEVITATION
                    durationInt = Integer.MAX_VALUE
                    ambient = true
                    key = NamespacedKey("terix", KEY)
                }
            )
        }
    }

    override suspend fun onDeactivate(player: Player) {
        glideMap -= player.uniqueId
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
        if (glideMap.contains(player.uniqueId)) return

        sentryBreadcrumb(SCOPE, "levitate.gliding.start")

        glideMap += player.uniqueId
        player.isGliding = true
    }

    companion object {
        const val KEY = "origin_ability_levitate"
        val SOUND = Triple("minecraft:entity.phantom.flap", 1f, 1f)
    }
}
