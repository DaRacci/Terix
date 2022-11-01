package dev.racci.terix.api.origins.sounds

public data class SoundEffects(
    var hurtSound: SoundEffect = SoundEffect("entity.player.hurt"),
    var deathSound: SoundEffect = SoundEffect("entity.player.death"),
    var ambientSound: SoundEffect? = null
)
