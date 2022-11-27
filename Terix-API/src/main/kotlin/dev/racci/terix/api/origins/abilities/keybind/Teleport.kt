package dev.racci.terix.api.origins.abilities.keybind

import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.origins.origin.Origin
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.FluidCollisionMode
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.util.BlockIterator
import org.bukkit.util.Vector
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public class Teleport(
    override val abilityPlayer: TerixPlayer,
    override val linkedOrigin: Origin,
    override val cooldownDuration: Duration = 3.seconds
) : TriggeringKeybindAbility() {

    override suspend fun handleTrigger() {
        val loc = this.getTargetLocation() ?: return this.invalidLocation()
        val sequence = BlockIterator(abilityPlayer.world, loc.toVector(), Vector(0, -90, 0), 0.0, 32).asSequence()
        val block = sequence.firstOrNull { it.type.isSolid } ?: return this.invalidLocation()

        this.correctLocation(loc, abilityPlayer, block)
        abilityPlayer.teleportAsync(loc)
    }

    private fun getTargetLocation(): Location? {
        val rayTrace = abilityPlayer.rayTraceBlocks(64.0, FluidCollisionMode.NEVER) ?: return null
        val hitLocation = rayTrace.hitPosition.toLocation(abilityPlayer.world)
        // Align to the eye direction
        hitLocation.add(abilityPlayer.eyeLocation.direction.normalize().multiply(-0.7))

        return hitLocation
    }

    private fun invalidLocation() {
        this.failActivation()
        return abilityPlayer.playSound(INVALID_LOCATION)
    }

    private fun correctLocation(
        loc: Location,
        player: Player,
        block: Block
    ) {
        loc.y = block.y.plus(1).toDouble()
        loc.direction = player.location.direction
    }

    private companion object {
        val INVALID_LOCATION: Sound = Sound.sound(Key.key("block.note_block.bass"), Sound.Source.MASTER, 1.0f, 0.5f)
    }
}
