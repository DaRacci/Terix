package me.racci.sylphia.factories

import me.racci.sylphia.enums.Condition
import me.racci.sylphia.utils.PotionUtils
import me.racci.sylphia.utils.PrivatePotionEffectType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

internal object PotionFactory {

    lateinit var levitationPotion: PotionEffect

    fun init() {
        levitationPotion = PotionEffect(
            PotionEffectType.LEVITATION,
            Int.MAX_VALUE,
            1,
            true,
            false,
            false,
            Condition.LEVITATION.toNamespacedkey()
        )
    }

    fun newPotion(string: String, condition: Condition) : PotionEffect? {
        val potion = string.split(":".toRegex()).toTypedArray()
        if ((potion[1].toIntOrNull() ?: return null) !in 0..if(PotionUtils.isValid(potion[0])) PrivatePotionEffectType.valueOf(potion[0]).maxLevel else return null) return null
        return PotionEffect(PotionEffectType.getByName(potion[0])!!, Int.MAX_VALUE, potion[1].toInt(), true, false, false, condition.toNamespacedkey())
    }


}