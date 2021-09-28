package me.racci.sylphia.origins.abilityevents

import me.racci.raccicore.events.PlayerDoubleOffhandEvent
import me.racci.raccicore.skedule.skeduleSync
import me.racci.raccicore.utils.strings.colour
import me.racci.sylphia.extensions.PlayerExtension.currentOrigin
import me.racci.sylphia.extensions.PlayerExtension.hasOrigin
import me.racci.sylphia.factories.PotionFactory
import me.racci.sylphia.lang.Lang
import me.racci.sylphia.lang.Prefix
import me.racci.sylphia.potionFactory
import me.racci.sylphia.sylphia
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.potion.PotionEffectType

class OffhandListener : Listener {

    @EventHandler
    fun onDoubleOffhand(event: PlayerDoubleOffhandEvent) {
        val player = event.player
        if(!player.hasOrigin) return
        when(player.currentOrigin!!.name.uppercase()) {
            "ANGEL", "SKVADER" -> {
                if(player.hasMetadata("Levitating")) {
                    skeduleSync(sylphia) {
                        player.removePotionEffect(PotionEffectType.LEVITATION)
                        player.setMetadata("Levitating", FixedMetadataValue(sylphia, false))
                    }
                } else if(player.saturation > 0.5) {
                    skeduleSync(sylphia) {
                        player.addPotionEffect(potionFactory.levitationPotion)
                        player.setMetadata("Levitating", FixedMetadataValue(sylphia, true))
                    }
                } else {
                    player.sendMessage(colour("${Lang.Messages.get(Prefix.ERROR)} &cYou don't have enough hunger to use this!")!!)
                }
            }



            else -> return
        }





    }


}