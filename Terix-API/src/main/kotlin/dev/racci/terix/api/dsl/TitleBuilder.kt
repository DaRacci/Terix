package dev.racci.terix.api.dsl

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player

class TitleBuilder(
    title: Component? = null,
    subtitle: Component? = null,
    times: Title.Times? = null,
    sound: Key? = null
) : CachingBuilder<Title>() {

    var title by createWatcher(title ?: Component.empty())
    var subtitle by createWatcher(subtitle ?: Component.empty())
    var times by createWatcher(times ?: Title.DEFAULT_TIMES)
    var sound by createWatcher(sound)

    override fun create() = Title.title(
        title,
        subtitle,
        times
    )

    fun invoke(player: Player) {
        player.showTitle(get())
        ::sound.watcherOrNull()?.let { player.playSound(Sound.sound(it, Sound.Source.AMBIENT, 1f, 1f)) }
    }
}
