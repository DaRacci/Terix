package dev.racci.terix.api.extensions

import dev.racci.minix.nms.aliases.toNMS
import net.minecraft.network.protocol.game.ClientboundCustomSoundPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundSource
import net.minecraft.world.phys.Vec3
import org.bukkit.Location
import org.bukkit.entity.Player

fun Location.playSound(
    soundKey: String,
    volume: Float = 1.0f,
    pitch: Float = 1.0f,
    distance: Double = 16.0,
    source: Player? = null,
) {
    val nmsWorld = this.world.toNMS()
    val packet = ClientboundCustomSoundPacket(
        ResourceLocation(soundKey),
        SoundSource.PLAYERS,
        Vec3(this.x, this.y, this.z),
        volume,
        pitch,
    )

    nmsWorld.server.playerList
        .broadcast(
            source?.toNMS(),
            this.x, this.y, this.z,
            distance,
            nmsWorld.dimension(),
            packet
        )
}
