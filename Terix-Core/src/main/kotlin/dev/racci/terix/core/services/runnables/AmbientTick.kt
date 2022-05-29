package dev.racci.terix.core.services.runnables

import dev.racci.minix.api.utils.now
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.extensions.playSound
import dev.racci.terix.api.origins.sounds.SoundEffect
import dev.racci.terix.core.services.RunnableService
import kotlinx.datetime.Instant
import org.bukkit.entity.Player
import kotlin.math.pow

class AmbientTick(
    private val player: Player,
    private val sound: SoundEffect,
    private val service: RunnableService,
    mother: MotherCoroutineRunnable
) : ChildCoroutineRunnable(mother) {

    private var disabled: Boolean = false
    private var lastAmbient: Instant = Instant.DISTANT_FUTURE

    private fun chancePass(): Boolean {
        return player.toNMS().random.nextDouble(0.0, 1.0).pow((now() - lastAmbient).inWholeSeconds.toDouble()) > 0.5
    }

    override suspend fun run() {
        if (disabled) return
        if (!chancePass()) return

        player.location.playSound(
            sound.resourceKey.asString(),
            sound.volume,
            sound.pitch,
            sound.distance,
            player
        )
    }
}
