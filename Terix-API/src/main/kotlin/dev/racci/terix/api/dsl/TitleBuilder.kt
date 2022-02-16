package dev.racci.terix.api.dsl

import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
class TitleBuilder(
    var title: Component? = null,
    var subtitle: Component? = null,
    var times: Title.Times? = null,
    var sound: Key? = null
) {
    private var cachedTitle: Title? = null

    fun build(): Title {
        validate(this) {
            validate(TitleBuilder::title).isNotNull()
            validate(TitleBuilder::subtitle).isNotNull()
        }
        cachedTitle = Title.title(
            title!!,
            subtitle!!,
            times ?: Title.DEFAULT_TIMES
        )
        return cachedTitle!!
    }

    fun invoke(player: Player) {
        player.showTitle(cachedTitle ?: build())
        sound?.let { player.playSound(Sound.sound(it, Sound.Source.AMBIENT, 1f, 1f)) }
    }
}
