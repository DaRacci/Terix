package me.racci.sylphia.factories

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound

class SoundFactory {

    lateinit var errorSound: Sound

    init {
        reload()
    }

    private fun reload() {

        errorSound = Sound.sound(Key.key(
            "block.note_block.bass"),
            Sound.Source.PLAYER,
            0.7f, 1f
        )


    }

}