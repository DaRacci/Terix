package dev.racci.terix.api.origins.sounds

data class SoundEffects(
    var hurtSound: SoundEffect = SoundEffect("entity.player.hurt"),
    var deathSound: SoundEffect = SoundEffect("entity.player.death"),
    var ambientSound: SoundEffect? = null,
)
