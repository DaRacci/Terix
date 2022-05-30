package dev.racci.terix.api.extensions

import dev.racci.minix.nms.aliases.toNMS
import net.minecraft.network.protocol.game.ClientboundCustomSoundPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundSource
import net.minecraft.world.phys.Vec3
import org.bukkit.entity.Player

fun Player.playSound(
    soundKey: String,
    volume: Float = 1.0f,
    pitch: Float = 1.0f,
    distance: Double = 16.0,
) {
    val nmsWorld = this.world.toNMS()

    val namespace = soundKey.substringBefore(':', "minecraft")
    val path = soundKey.substringAfter(':')
    val resourceKey = ResourceLocation(namespace, path)

    val packet = ClientboundCustomSoundPacket(
        resourceKey,
        SoundSource.PLAYERS,
        Vec3(this.location.x, this.location.y, this.location.z),
        volume, pitch,
    )

    nmsWorld.server.playerList
        .broadcast(
            this.toNMS(),
            this.location.x, this.location.y, this.location.z,
            if (volume > 1f) volume * distance else distance,
            nmsWorld.dimension(),
            packet
        )

    // Broadcasting with the player as the source skips sending the packet to themself.
    this.toNMS().networkManager.send(packet)
}
