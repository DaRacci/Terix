package dev.racci.terix.core.services

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.wrappers.EnumWrappers
import com.comphenix.protocol.wrappers.MinecraftKey
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.utils.kotlin.and
import dev.racci.terix.api.Terix
import dev.racci.terix.core.extension.origin
import kotlinx.collections.immutable.persistentListOf
import org.bukkit.Sound

class SoundService(override val plugin: Terix) : Extension<Terix>() {

    override val name get() = "Sound Service"
    override val dependencies get() = persistentListOf(OriginService::class)

    private val protocolManager by lazy(ProtocolLibrary::getProtocolManager)
    private val lazyCollection by lazy { Sound.ENTITY_PLAYER_HURT and Sound.ENTITY_PLAYER_DEATH }

    override suspend fun handleEnable() {
        protocolManager.addPacketListener(
            object : PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.NAMED_SOUND_EFFECT) {
                override fun onPacketSending(event: PacketEvent) {
                    if (event.packet.soundCategories.readSafely(0) == EnumWrappers.SoundCategory.PLAYERS) {
                        val effect = event.packet.soundEffects.readSafely(0)
                        if (effect !in lazyCollection) return
                        val origin = event.player.origin()
                        val sound = when (effect) {
                            Sound.ENTITY_PLAYER_HURT -> origin.hurtSound
                            Sound.ENTITY_PLAYER_DEATH -> origin.deathSound
                            else -> return
                        }
                        event.packet.minecraftKeys.write(
                            0,
                            MinecraftKey(sound.namespace(), sound.value())
                        )
                    }
                }
            }
        )
    }

    override suspend fun handleUnload() {
        protocolManager.packetListeners.forEach(protocolManager::removePacketListener)
    }
}
