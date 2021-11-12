package me.racci.sylphia.managers

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.ProtocolManager
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.wrappers.EnumWrappers
import me.racci.sylphia.Sylphia
import org.bukkit.Sound.ENTITY_PLAYER_HURT

internal object SoundManager {

    var protocolManager: ProtocolManager? = null

    fun init() {
        if(Sylphia.protocolLib) {
            protocolManager = ProtocolLibrary.getProtocolManager()
            protocolManager!!.addPacketListener(object :
                PacketAdapter(Sylphia.instance, ListenerPriority.NORMAL, PacketType.Play.Server.NAMED_SOUND_EFFECT) {
                override fun onPacketSending(event: PacketEvent) {
                    if (event.packet.soundCategories.readSafely(0) == EnumWrappers.SoundCategory.PLAYERS) {
                        when (event.packet.soundEffects.readSafely(0)) {
                            ENTITY_PLAYER_HURT -> event.isCancelled = true
                            else -> return
                        }
                    }
                }
            })
        }
    }

    fun close() {
        protocolManager?.packetListeners?.forEach(protocolManager!!::removePacketListener)
    }

}