package dev.racci.terix.api.origins.abilities.keybind

import dev.racci.minix.api.events.player.PlayerMoveXYZEvent
import dev.racci.minix.api.extensions.cancel
import dev.racci.terix.api.annotations.OriginEventSelector
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.dsl.dslMutator
import dev.racci.terix.api.extensions.playSound
import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.services.TickService
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import kotlin.time.Duration

public class Levitate(
    override val abilityPlayer: Player,
    override val linkedOrigin: Origin
) : TogglingKeybindAbility() {
    private var glidingActive: Boolean = false
    private lateinit var lastVector: Vector

    // TODO -> Apply velocity
    @OriginEventSelector(EventSelector.PLAYER)
    public fun PlayerMoveXYZEvent.ensureGliding() {
        if (!isActivated || glidingActive) return

        val difference = from.subtract(to)
        if (difference.x in -0.1..0.1 && difference.y != 0.0 && difference.z in -0.1..0.1) return // If the player is only moving vertically, don't activate gliding.

        abilityBreadcrumb("glidingActivate")
        glidingActive = true
        player.isGliding = true
    }

    @OriginEventSelector(EventSelector.ENTITY, ignoreCancelled = false)
    public fun EntityToggleGlideEvent.cancelReversion() {
        if (isActivated) cancel()
    }

    override suspend fun handleAbilityGained() {
        TickService.filteredPlayer(abilityPlayer)
            .filter { isActivated }
            .filter {
                val newVector = abilityPlayer.location.toVector()

                if (!::lastVector.isInitialized) {
                    lastVector = newVector
                    return@filter false
                }

                val difference = lastVector.clone().subtract(newVector)
                lastVector = newVector
                difference.x in -0.1..0.1 && difference.z in -0.1..0.1
            }.onEach { player ->
                abilityBreadcrumb("glidingDeactivate")
                player.isGliding = false
                glidingActive = false
            }.abilitySubscription()
    }

    override suspend fun handleActivation() {
        abilityPlayer.playSound(SOUND.first, SOUND.second, SOUND.third)
        sync { abilityPlayer.addPotionEffect(taggedPotion(LEVITATION)) }
    }

    override suspend fun handleDeactivation() {
        glidingActive = false
        val types = mutableListOf<PotionEffectType>()
        for (potion in abilityPlayer.activePotionEffects) {
            if (potion.type != PotionEffectType.LEVITATION || !isTagged(potion)) continue
            types += potion.type
            break
        }

        abilityPlayer.playSound(SOUND.first, SOUND.second, SOUND.third)
        sync { types.forEach(abilityPlayer::removePotionEffect) }
    }

    private companion object {
        val SOUND = Triple("minecraft:entity.phantom.flap", 1f, 1f)
        val LEVITATION = dslMutator<PotionEffectBuilder> {
            type = PotionEffectType.LEVITATION
            duration = Duration.INFINITE
            ambient = true
        }.asNew()
    }
}
