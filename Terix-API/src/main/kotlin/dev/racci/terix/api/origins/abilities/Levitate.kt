package dev.racci.terix.api.origins.abilities

import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.ensureMainThread
import dev.racci.terix.api.extensions.playSound
import dev.racci.terix.api.origins.AbstractAbility
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType

class Levitate : AbstractAbility() {

    override suspend fun onActivate(player: Player) {
        ensureMainThread {
            player.velocity.add(player.location.direction.multiply(0.1))
            player.addPotionEffect(
                PotionEffectBuilder.build {
                    type = PotionEffectType.LEVITATION
                    durationInt = Integer.MAX_VALUE
                    ambient = true
                    key = NamespacedKey("terix", KEY)
                }
            )
            player.location.playSound("entity.phantom.flap", 1f, 1f, source = player)
        }
    }

    override suspend fun onDeactivate(player: Player) {
        val types = mutableListOf<PotionEffectType>()
        for (potion in player.activePotionEffects) {
            if (potion.type != PotionEffectType.LEVITATION || potion.key?.key != KEY) continue
            types += potion.type
            break
        }

        ensureMainThread { types.forEach(player::removePotionEffect) }
    }

    companion object {
        const val KEY = "origin_ability_levitate"
    }
}
