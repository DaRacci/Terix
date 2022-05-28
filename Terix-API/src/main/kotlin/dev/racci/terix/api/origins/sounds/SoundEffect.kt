package dev.racci.terix.api.origins.sounds

import net.kyori.adventure.key.Key

class SoundEffect(
    val resourceKey: Key,
    val volume: Float = 1f,
    val pitch: Float = 1f,
    val distance: Double = 16.0,
) {
    constructor(
        resourceKey: String,
        volume: Float = 1f,
        pitch: Float = 1f,
        distance: Double = 16.0,
    ) : this(Key.key(resourceKey), volume, pitch, distance)
}
