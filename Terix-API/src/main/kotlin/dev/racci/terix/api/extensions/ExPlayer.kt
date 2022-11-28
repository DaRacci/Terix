package dev.racci.terix.api.extensions

import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.data.OriginNamespacedTag
import net.minecraft.network.protocol.game.ClientboundCustomSoundPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.phys.Vec3
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect

public val Player.handle: ServerPlayer get() = if (this is TerixPlayer) this.handle else this.toNMS()

internal fun Player.originPotions(filterOrigin: Boolean = true): Sequence<Pair<PotionEffect, OriginNamespacedTag>> {
    val origin by lazy { TerixPlayer.cachedOrigin(this) }
    return this.activePotionEffects.asSequence()
        .filter(PotionEffect::hasKey)
        .mapNotNull { pot -> pot to (OriginNamespacedTag.fromBukkitKey(pot.key!!) ?: return@mapNotNull null) }
        .filter { (_, tag) -> !filterOrigin || tag.fromOrigin(origin) }
}

public val Player.originPassivePotions: Sequence<PotionEffect> get() = this.originPotions()
    .filter { (_, tag) -> tag.isSourceBase }
    .map { (pot, _) -> pot }

public val Player.originAbilityPotions: Sequence<PotionEffect> get() = this.originPotions()
    .filter { (_, match) -> match.isSourceAbility }
    .map { (pot, _) -> pot }

public val Player.originFoodPotions: Sequence<PotionEffect> get() = this.originPotions()
    .filter { (_, match) -> match.isSourceFood }
    .map { (pot, _) -> pot }

public val Player.allOriginPotions: Sequence<PotionEffect> get() = this.activePotionEffects.asSequence()
    .filter(PotionEffect::hasKey)
    .filter { pot -> OriginNamespacedTag.REGEX.matches(pot.key!!.asString()) }

public val Player.originPassiveModifiers: Map<AttributeInstance, List<AttributeModifier>>
    get() = Attribute.values().asSequence()
        .mapNotNull { attr -> this.getAttribute(attr) }
        .associateWith { attr -> attr.modifiers.filter { mod -> OriginNamespacedTag.REGEX.matches(mod.name) } }
        .filterValues { mods -> mods.isNotEmpty() }

public fun Player.playSound(
    soundKey: String,
    volume: Float = 1.0f,
    pitch: Float = 1.0f,
    distance: Double = 16.0
) {
    val nmsWorld = this.world.toNMS()

    val namespace = soundKey.substringBefore(':', "minecraft")
    val path = soundKey.substringAfter(':')
    val resourceKey = ResourceLocation(namespace, path)

    val packet = ClientboundCustomSoundPacket(
        resourceKey,
        SoundSource.PLAYERS,
        Vec3(this.location.x, this.location.y, this.location.z),
        volume,
        pitch,
        0
    )

    nmsWorld.server.playerList
        .broadcast(
            this.handle,
            this.location.x,
            this.location.y,
            this.location.z,
            if (volume > 1f) volume * distance else distance,
            nmsWorld.dimension(),
            packet
        )

    // Broadcasting with the player as the source skips sending the packet to themself.
    this.handle.networkManager.send(packet)
}
