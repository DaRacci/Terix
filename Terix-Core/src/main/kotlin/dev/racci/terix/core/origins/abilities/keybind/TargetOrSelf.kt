package dev.racci.terix.core.origins.abilities.keybind

import arrow.core.filterIsInstance
import arrow.core.orElse
import arrow.core.toOption
import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.origins.abilities.keybind.TriggeringKeybindAbility
import dev.racci.terix.api.origins.origin.Origin
import org.bukkit.entity.LivingEntity
import kotlin.time.Duration

/**
 * Takes a lambda which will be run when the ability is triggered.
 * If no target LivingEntity can be found the target will be the ability player.
 *
 * @property abilityPlayer
 * @property linkedOrigin The origin, which this ability is linked to.
 * @property cooldownDuration The duration of the cooldown.
 * @property searchDistance The distance to search for a target.
 * @property lambda The lambda to run when the ability is triggered.
 */
public class TargetOrSelf(
    override val linkedOrigin: Origin,
    override val abilityPlayer: TerixPlayer,
    override val cooldownDuration: Duration,
    public val searchDistance: Int,
    public val lambda: suspend (LivingEntity) -> Unit
) : TriggeringKeybindAbility() {
    override suspend fun handleTrigger() {
        abilityPlayer.getTargetEntity(searchDistance).toOption()
            .filterIsInstance<LivingEntity>()
            .orElse(abilityPlayer::toOption)
            .filter { target -> target.isDead }
            .fold(
                ifEmpty = ::failActivation,
                ifSome = { target -> lambda(target) }
            )
    }
}
