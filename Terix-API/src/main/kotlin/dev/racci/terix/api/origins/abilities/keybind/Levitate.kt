package dev.racci.terix.api.origins.abilities.keybind

import dev.racci.minix.api.events.player.PlayerMoveXYZEvent
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.utils.kotlin.invokeIfNull
import dev.racci.terix.api.annotations.OriginEventSelector
import dev.racci.terix.api.data.player.TerixPlayer
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.dsl.dslMutator
import dev.racci.terix.api.extensions.playSound
import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.services.TickService
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import kotlin.time.Duration

public class Levitate(
    override val abilityPlayer: TerixPlayer,
    override val linkedOrigin: Origin,
    override val cooldownDuration: Duration = Duration.ZERO
) : TogglingKeybindAbility() {
    private val levitatePotion = LEVITATION.asNew().applyTag()
    private var glidingActive: Boolean = false
    private lateinit var lastVector: Vector

    // TODO -> Apply velocity
    @OriginEventSelector(EventSelector.PLAYER)
    public fun PlayerMoveXYZEvent.ensureGliding() {
        if (!isActivated || glidingActive) return

        val difference = from.subtract(to)
        if (difference.x in DIFFERENCE_RANGE && /*difference.y != 0.0 &&*/ difference.z in DIFFERENCE_RANGE) return // If the player is only moving vertically, don't activate gliding.

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
                difference.x in DIFFERENCE_RANGE && difference.z in DIFFERENCE_RANGE
            }.filter { glidingActive }.onEach { player ->
                abilityBreadcrumb("glidingDeactivate")
                player.isGliding = false
                glidingActive = false
            }.abilitySubscription()
    }

    override suspend fun handleActivation() {
        abilityPlayer.playSound(SOUND.first, SOUND.second, SOUND.third)
        sync { levitatePotion(abilityPlayer) }
    }

    override suspend fun handleDeactivation() {
        glidingActive = false
        abilityPlayer.isGliding = false
        abilityPlayer.playSound(SOUND.first, SOUND.second, SOUND.third)
        abilityPlayer.activePotionEffects.asSequence()
            .filter { pot -> pot.type == PotionEffectType.LEVITATION }
            .firstOrNull { pot -> isTagged(pot) }
            .invokeIfNull { sync { abilityPlayer.removePotionEffect(PotionEffectType.LEVITATION) } }
    }

    private companion object {
        const val DIFFERENCE_THRESHOLD = 0.05
        val DIFFERENCE_RANGE = -DIFFERENCE_THRESHOLD..DIFFERENCE_THRESHOLD
        val SOUND = Triple("minecraft:entity.phantom.flap", 1f, 1f)
        val LEVITATION = dslMutator<PotionEffectBuilder> {
            type = PotionEffectType.LEVITATION
            duration = Duration.INFINITE
            ambient = true
        }
    }
}
