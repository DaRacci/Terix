package me.racci.sylphia.factories

import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class PotionFactory {

    lateinit var levitationPotion: PotionEffect

    init {
        reload()
    }

    private fun reload() {
        levitationPotion = PotionEffect(
            PotionEffectType.LEVITATION,
            Int.MAX_VALUE,
            1,
            true,
            false,
            false
        )




    }


}