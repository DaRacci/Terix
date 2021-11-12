package me.racci.sylphia.factories

import me.racci.raccicore.interfaces.IFactory
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound

internal object SoundFactory: IFactory<SoundFactory> {

    lateinit var errorSound: Sound

    override fun init() {

        errorSound = Sound.sound(Key.key(
            "block.note_block.bass"),
            Sound.Source.PLAYER,
            0.7f, 1f
        )

    }

    override fun reload() {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

}