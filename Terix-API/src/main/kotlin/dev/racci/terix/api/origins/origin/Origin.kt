package dev.racci.terix.api.origins.origin

import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.plugin.MinixPlugin
import org.apiguardian.api.API
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent

@API(status = API.Status.MAINTAINED, since = "1.0.0")
abstract class Origin : OriginBuilder(), OriginEventListener, WithPlugin<MinixPlugin>, KoinComponent {

    /**
     * Checks if the player has permission for this origin.
     *
     * @param player The player to check.
     * @return True if the player has permission, false otherwise.
     */
    open suspend fun hasPermission(player: Player) = permission?.let(player::hasPermission) ?: true

    override fun toString(): String {
        return "Origin(name='$name', item=$item, " +
            "abilities=$abilities, stateBlock=$stateBlocks, stateDamageTicks=$stateDamageTicks, " +
            "damageActions=$damageActions, customFoodProperties=$customFoodProperties, customFoodActions=$customFoodActions)"
    }
}
