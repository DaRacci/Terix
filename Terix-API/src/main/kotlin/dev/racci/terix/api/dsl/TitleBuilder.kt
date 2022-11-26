package dev.racci.terix.api.dsl

import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player

public class TitleBuilder(
    title: Component? = null,
    subtitle: Component? = null,
    times: Title.Times? = null,
    sound: Key? = null
) : CachingBuilder<Title>() {

    public var title: Component by createWatcher(title ?: Component.empty())
    public var subtitle: Component by createWatcher(subtitle ?: Component.empty())
    public var times: Title.Times by createWatcher(times ?: Title.DEFAULT_TIMES)
    public var sound: Key by createWatcher(sound)

    override fun create(): Title = Title.title(
        title,
        subtitle,
        times
    )

    public operator fun invoke(player: Player) {
        player.showTitle(get())
        ::sound.watcherOrNull()?.let { player.playSound(Sound.sound(it, Sound.Source.AMBIENT, 1f, 1f)) }
    }
}
