package dev.racci.terix.api.origins.sounds

import net.kyori.adventure.key.Key

public class SoundEffect(
    public val resourceKey: Key,
    public val volume: Float = 1f,
    public val pitch: Float = 1f,
    public val distance: Double = 16.0
) {
    public constructor(
        resourceKey: String,
        volume: Float = 1f,
        pitch: Float = 1f,
        distance: Double = 16.0
    ) : this(Key.key(resourceKey), volume, pitch, distance)
}
