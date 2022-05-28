package dev.racci.terix.core.services

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.wrappers.EnumWrappers
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.utils.kotlin.and
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.Terix
import org.bukkit.Sound
import org.koin.core.component.inject

@MappedExtension(Terix::class, "Sound Service", [OriginService::class, HookService::class])
class SoundService(override val plugin: Terix) : Extension<Terix>() {
    private val hookService by inject<HookService>()
    private val lazyCollection by lazy { Sound.ENTITY_PLAYER_HURT and Sound.ENTITY_PLAYER_DEATH }

    override suspend fun handleEnable() {
        hookService.protocolManager.addPacketListener(
            object : PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.NAMED_SOUND_EFFECT) {
                override fun onPacketSending(event: PacketEvent) {
                    if (event.packet.soundCategories.read(0) == EnumWrappers.SoundCategory.PLAYERS &&
                        event.packet.soundEffects.read(0) in lazyCollection
                    ) {
                        log.debug { "Cancelling sound packet from ${event.player.name}" }
                        event.cancel()
                    }
                }
            }
        )
    }

    override suspend fun handleUnload() {
        hookService.protocolManager.packetListeners.forEach(hookService.protocolManager::removePacketListener)
    }
}
