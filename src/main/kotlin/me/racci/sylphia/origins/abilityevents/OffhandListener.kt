package me.racci.sylphia.origins.abilityevents

import com.github.shynixn.mccoroutine.minecraftDispatcher
import kotlinx.coroutines.withContext
import me.racci.raccicore.events.PlayerDoubleOffhandEvent
import me.racci.raccicore.events.PlayerOffhandEvent
import me.racci.raccicore.utils.extensions.KotlinListener
import me.racci.raccicore.utils.strings.colour
import me.racci.raccicore.utils.strings.textOf
import me.racci.sylphia.Sylphia
import me.racci.sylphia.extensions.PlayerExtension.currentOrigin
import me.racci.sylphia.extensions.PlayerExtension.hasOrigin
import me.racci.sylphia.factories.PotionFactory
import me.racci.sylphia.factories.SoundFactory
import me.racci.sylphia.lang.Lang
import me.racci.sylphia.lang.Prefix
import org.bukkit.FluidCollisionMode
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.BlockIterator
import org.bukkit.util.Vector


class OffhandListener(private val plugin: Sylphia) : KotlinListener {

    @EventHandler
    suspend fun onDoubleOffhand(event: PlayerDoubleOffhandEvent) {
        val player = event.player
        if(!player.hasOrigin) return
        when(player.currentOrigin!!.identity.name.uppercase()) {
            "ANGEL", "SKVADER" -> {
                if(player.hasMetadata("Levitating")) {
                    withContext(plugin.minecraftDispatcher) {
                        player.removePotionEffect(PotionEffectType.LEVITATION)
                        player.removeMetadata("Levitating", plugin)
                    }
                } else if(player.foodLevel > 0.5) {
                    withContext(plugin.minecraftDispatcher) {
                        player.foodLevel.minus(0.5)
                        player.addPotionEffect(PotionFactory.levitationPotion)
                        player.setMetadata("Levitating", FixedMetadataValue(plugin, true))
                    }
                } else {
                    player.sendMessage(colour("${Lang[Prefix.ERROR]} &cYou don't have enough hunger to use this!"))
                    player.playSound(SoundFactory.errorSound)

                }
            }
            else -> return
        }
    }

    @EventHandler
    suspend fun onOffhand(event: PlayerOffhandEvent) {
        val player = event.player
        if(!player.hasOrigin) return
        when(player.currentOrigin!!.identity.name.uppercase()) {
            "ENDER" -> {
                if(!player.hasMetadata("TeleportCooldown") || ((System.currentTimeMillis() - player.getMetadata("TeleportCooldown")[0].asLong()) >= 3000)) {
                    val rayTrace = player.rayTraceBlocks(64.0, FluidCollisionMode.NEVER)
                    if(rayTrace == null) {
                        player.sendMessage(colour("${Lang[Prefix.ERROR]} &cThe target location is too far away!"))
                        player.playSound(SoundFactory.errorSound)
                        return
                    }
                    val loc = rayTrace.hitPosition.toLocation(player.world)
                    loc.add(player.eyeLocation.direction.normalize().multiply(-0.7))
                    val iterator = BlockIterator(player.world, loc.toVector(), Vector(0, -90, 0), 0.0, 32)
                    var block: Block? = null
                    while(iterator.hasNext()) {
                        val var1 = iterator.next()
                        if(var1.isSolid) {
                            block = var1
                            break
                        }
                    }
                    loc.y = (block?.y?.plus(1))?.toDouble() ?: return
                    loc.direction = player.location.direction
                    player.setMetadata("TeleportCooldown", FixedMetadataValue(plugin, System.currentTimeMillis()))
                    withContext(plugin.minecraftDispatcher) {
                        player.teleportAsync(loc)
                        player.sendActionBar(textOf("&5~Vwoop"))
                    }
                }
            }
        }
    }
}