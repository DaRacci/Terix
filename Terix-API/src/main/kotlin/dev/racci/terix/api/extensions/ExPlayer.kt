package dev.racci.terix.api.extensions

import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.dsl.AttributeModifierBuilder
import dev.racci.terix.api.dsl.PotionEffectBuilder
import net.minecraft.network.protocol.game.ClientboundCustomSoundPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundSource
import net.minecraft.world.phys.Vec3
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect

private fun Player.basePotions(): Sequence<Pair<PotionEffect, MatchResult>> {
    val origin = TerixPlayer.cachedOrigin(this)
    return this.activePotionEffects.asSequence()
        .filter(PotionEffect::hasKey)
        .mapNotNull { pot ->
            val match = PotionEffectBuilder.regex.find(pot.key!!.asString()) ?: return@mapNotNull null
            if (match.groups["origin"]!!.value != origin.name.lowercase()) return@mapNotNull null
            pot to match
        }
}

public val Player.originPassivePotions: Sequence<PotionEffect> get() = this.basePotions()
    .filter { (_, match) -> match.groups["type"]!!.value == "potion" }
    .map { (pot, _) -> pot }

public val Player.originAbilityPotions: Sequence<PotionEffect> get() = this.basePotions()
    .filter { (_, match) -> match.groups["type"]!!.value == "ability" }
    .map { (pot, _) -> pot }

public val Player.originFoodPotions: Sequence<PotionEffect>
    get() = this.basePotions()
        .filter { (_, match) -> match.groups["type"]!!.value == "food" }
        .map { (pot, _) -> pot }

public val Player.allOriginPotions: Sequence<PotionEffect>
    get() = this.activePotionEffects.asSequence()
        .filter(PotionEffect::hasKey)
        .filter { pot -> PotionEffectBuilder.regex.matches(pot.key!!.asString()) }

public val Player.originPassiveModifiers: Map<AttributeInstance, List<AttributeModifier>>
    get() = Attribute.values().asSequence()
        .mapNotNull { attr -> this.getAttribute(attr) }
        .associateWith { attr -> attr.modifiers.filter { mod -> mod.name.matches(AttributeModifierBuilder.regex) } }
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
            this.toNMS(),
            this.location.x,
            this.location.y,
            this.location.z,
            if (volume > 1f) volume * distance else distance,
            nmsWorld.dimension(),
            packet
        )

    // Broadcasting with the player as the source skips sending the packet to themself.
    this.toNMS().networkManager.send(packet)
}
