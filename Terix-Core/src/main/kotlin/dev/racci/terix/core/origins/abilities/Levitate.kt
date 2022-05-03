package dev.racci.terix.core.origins.abilities

import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.origins.AbstractAbility
import dev.racci.terix.core.extension.removePotionWithKey
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType

class Levitate : AbstractAbility() {

    override fun onActivate(player: Player) {
        player.velocity.add(player.location.direction.multiply(0.1))
        player.addPotionEffect(
            PotionEffectBuilder.build {
                type = PotionEffectType.LEVITATION
                durationInt = Integer.MAX_VALUE
                ambient = true
                key = NamespacedKey("terix", KEY)
            }
        )
    }

    override fun onDeactivate(player: Player) {
        player.removePotionWithKey(PotionEffectType.LEVITATION, KEY)
    }

    companion object {
        const val KEY = "origin_ability_levitate"
    }
}
