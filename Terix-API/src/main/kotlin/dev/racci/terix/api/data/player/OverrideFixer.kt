package dev.racci.terix.api.data.player

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.inventory.Book
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.sound.SoundStop
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.TitlePart

/** A group of overrides, which are overridden within [CraftPlayer] but not [Player] and need manual overrides. */
public sealed interface OverrideFixer : Audience {
    override fun sendActionBar(message: Component)
    override fun sendPlayerListHeader(header: Component)
    override fun sendPlayerListFooter(footer: Component)
    override fun sendPlayerListHeaderAndFooter(header: Component, footer: Component)
    override fun showTitle(title: Title)
    override fun <T : Any> sendTitlePart(part: TitlePart<T>, value: T)
    override fun clearTitle()
    override fun resetTitle()
    override fun showBossBar(bar: BossBar)
    override fun hideBossBar(bar: BossBar)
    override fun playSound(sound: Sound)
    override fun playSound(sound: Sound, x: Double, y: Double, z: Double)
    override fun playSound(sound: Sound, emitter: Sound.Emitter)
    override fun stopSound(stop: SoundStop)
    override fun openBook(book: Book)
}
