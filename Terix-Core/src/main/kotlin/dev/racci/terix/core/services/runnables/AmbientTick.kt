package dev.racci.terix.core.services.runnables

import dev.racci.minix.api.utils.now
import dev.racci.terix.api.extensions.playSound
import dev.racci.terix.api.origins.sounds.SoundEffect
import dev.racci.terix.core.services.RunnableService
import kotlinx.datetime.Instant
import org.bukkit.entity.Player
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class AmbientTick(
    private val player: Player,
    private val sound: SoundEffect,
    private val service: RunnableService,
    mother: MotherCoroutineRunnable
) : ChildCoroutineRunnable(mother) {

    private var disabled: Boolean = false
    private var nextAmbient: Instant = Instant.DISTANT_FUTURE

    private fun ambientTime(): Boolean {
        if (now() < nextAmbient) return false
        nextAmbient += Random.nextDouble(MIN_TIME, MAX_TIME).seconds
        return true
    }

    override suspend fun run() {
        if (disabled) return
        if (!ambientTime()) return

        player.playSound(
            sound.resourceKey.asString(),
            sound.volume,
            sound.pitch,
            sound.distance,
        )
    }

    companion object {
        const val MIN_TIME: Double = 600.0
        const val MAX_TIME: Double = 1200.0
    }
}
