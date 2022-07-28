package dev.racci.terix.api.origins.abilities

import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.sync
import dev.racci.minix.api.flow.eventFlow
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.extensions.playSound
import dev.racci.terix.api.origins.AbstractAbility
import dev.racci.terix.api.sentryBreadcrumb
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.takeWhile
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.potion.PotionEffectType
import java.util.UUID

class Levitate : AbstractAbility(AbilityType.TOGGLE) {

    private val glideMap = HashSet<UUID>()

    override suspend fun onActivate(player: Player) {
        sync {
            sentryBreadcrumb(player, "origin.abilities", "levitate.activate")

            startGliding(player)
            player.velocity.add(player.location.direction.multiply(0.3))
            player.playSound(SOUND.first, SOUND.second, SOUND.third)
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
        sentryBreadcrumb(player, "origin.abilities", "levitate.deactivate")

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

    private suspend fun startGliding(player: Player) {
        if (glideMap.contains(player.uniqueId)) return

        sentryBreadcrumb(player, "origin.abilities", "levitate.gliding.start")

        glideMap += player.uniqueId
        player.isGliding = true

        val channel = Channel<EntityToggleGlideEvent>(Channel.CONFLATED)

        try {
            eventFlow(player, EventPriority.HIGHEST, true, channel = channel)
                .takeWhile { glideMap.contains(player.uniqueId) }
                .filterNot { it.entity == player }
                .onCompletion { }
                .collect { plugin.log.debug { "Collected for ${player.name()}" }; it.cancel() }
        } finally {
            sentryBreadcrumb(player, "origin.abilities", "levitate.gliding.stop")
            channel.close()
        }
    }

    companion object {
        const val KEY = "origin_ability_levitate"
        val SOUND = Triple("minecraft:entity.phantom.flap", 1f, 1f)
    }
}
