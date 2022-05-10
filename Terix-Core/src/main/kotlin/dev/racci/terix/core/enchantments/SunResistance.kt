package dev.racci.terix.core.enchantments

import com.willfp.ecoenchants.enchantments.EcoEnchant
import com.willfp.ecoenchants.enchantments.meta.EnchantmentType

class SunResistance : EcoEnchant("sun-resistance", EnchantmentType.NORMAL) {
    init {
        instance = this
    }

    companion object {
        var instance: SunResistance? = null
    }
}
