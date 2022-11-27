package dev.racci.terix.core.services.runnables

import dev.racci.minix.api.utils.now
import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.extensions.playSound
import dev.racci.terix.api.origins.OriginHelper
import dev.racci.terix.api.origins.sounds.SoundEffect
import kotlinx.datetime.Instant
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

public class AmbientTick(terixPlayer: TerixPlayer) : ChildTicker(terixPlayer) {

    private var disabled: Boolean = false
    private var nextAmbient: Instant = Instant.DISTANT_FUTURE
    private val sound: SoundEffect = player.origin.sounds.ambientSound ?: error("No ambient sound for origin ${player.origin.name}")

    private fun ambientTime(): Boolean {
        if (now() < nextAmbient) return false
        nextAmbient += Random.nextDouble(MIN_TIME, MAX_TIME).seconds
        return true
    }

    override suspend fun handleRun() {
        if (disabled) return
        if (OriginHelper.shouldIgnorePlayer(player)) return
        if (!ambientTime()) return

        player.playSound(
            sound.resourceKey.asString(),
            sound.volume,
            sound.pitch,
            sound.distance
        )
    }

    private companion object {
        const val MIN_TIME: Double = 600.0
        const val MAX_TIME: Double = 1200.0
    }
}
