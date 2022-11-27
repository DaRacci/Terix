package dev.racci.terix.core.integrations

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent
import dev.lone.itemsadder.api.FontImages.FontImageWrapper
import dev.lone.itemsadder.api.FontImages.PlayerCustomHudWrapper
import dev.lone.itemsadder.api.FontImages.PlayerHudsHolderWrapper
import dev.racci.minix.api.annotations.MappedIntegration
import dev.racci.minix.api.coroutine.asyncDispatcher
import dev.racci.minix.api.coroutine.scope
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.plugin.MinixPlugin
import dev.racci.terix.api.Terix
import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.events.abilities.KeybindAbilityActivateEvent
import dev.racci.terix.api.origins.abilities.keybind.KeybindAbility
import dev.racci.terix.api.services.TickService
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import kotlin.math.roundToInt
import kotlin.time.Duration

@MappedIntegration(
    "ItemsAdder",
    Terix::class
)
public class ItemsAdderIntegration(override val plugin: MinixPlugin) : FileExtractorIntegration() {
    override suspend fun handleLoad() {
        if (extractDefaultAssets()) logger.warn { "Default assets have been extracted. Please run /iazip to finish the setup." }
    }

    override suspend fun handleEnable() {
        event<ItemsAdderLoadDataEvent> {
            val images = mutableListOf<FontImageWrapper>()
            var i = 0
            do {
                val id = "terix:ability_bar_$i"
                logger.debug { "Loading font image $id" }
                FontImageWrapper(id).also(images::add)
            } while (i++ < 50 && images.last().exists())

            if (images.isEmpty()) throw logger.fatal { "No font images found for ability bars!" }

            PlayerData.fontImages = images.filter(FontImageWrapper::exists).reversed().toTypedArray()
        }

        event<KeybindAbilityActivateEvent>(EventPriority.MONITOR, true) {
            val index = TerixPlayer.cachedOrigin(this.player).abilityData[this.player].indexOf(this.ability)
            PlayerData[this.player].tickAbility(index, this.ability)
        }

        event<PlayerOriginChangeEvent>(EventPriority.MONITOR, true) { PlayerData.cache.invalidate(player) }
        event<PlayerJoinEvent>(EventPriority.MONITOR, true) { PlayerData[this.player] }

        TickService.playerFlow
            .map(PlayerData::get)
            .onEach(PlayerData::tick)
            .launchIn(plugin.scope + plugin.asyncDispatcher)
    }

    override fun filterResource(name: String): Boolean = name.startsWith("contents/")

    private data class PlayerData(
        val playerRef: Player,
        val abilities: Array<KeybindAbility>
    ) {
        val holderWrapper: PlayerHudsHolderWrapper = PlayerHudsHolderWrapper(playerRef)
        val hudElements: Array<PlayerCustomHudWrapper> = Array(abilities.size) { index ->
            PlayerCustomHudWrapper(
                holderWrapper,
                "terix:ability_bar_$index"
            ).apply {
                this.offsetX += (fontImages.last().width + 15) * index
                this.isVisible = true
                this.floatValue = fontImages.lastIndex.toFloat()
                this.addFontImage(fontImages.last())
            }.also { holderWrapper.recalculateOffsets(); holderWrapper.sendUpdate() }
        }

        fun tick() {
            abilities.forEachIndexed { index, ability -> tickAbility(index, ability) }
        }

        fun tickAbility(
            index: Int,
            ability: KeybindAbility
        ) {
            if (ability.cooldown.expired()) return

            val element = hudElements[index]
            val cooldown = ability.cooldownDuration
            val remaining = ability.cooldown.remaining()
            val imageCount = fontImages.size.dec()
            val percent = (remaining / cooldown) * imageCount

            element.removeFontImageByIndex(0)
            element.addFontImageToIndex(fontImages[percent.roundToInt()], 0)

            element.floatValue = percent.toFloat()

            if (element.floatValue % imageCount != percent.toFloat() % imageCount) {
                holderWrapper.sendUpdate()
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PlayerData) return false

            if (playerRef != other.playerRef) return false
            if (!abilities.contentEquals(other.abilities)) return false
            if (holderWrapper != other.holderWrapper) return false
            if (!hudElements.contentEquals(other.hudElements)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = playerRef.hashCode()
            result = 31 * result + abilities.contentHashCode()
            result = 31 * result + holderWrapper.hashCode()
            result = 31 * result + hudElements.contentHashCode()
            return result
        }

        companion object {
            internal var fontImages: Array<FontImageWrapper> = emptyArray()
            internal val cache: LoadingCache<Player, PlayerData> = Caffeine.newBuilder().weakKeys()
                .evictionListener<Player, PlayerData> { _, value, _ ->
                    if (value == null) return@evictionListener

                    value.hudElements.forEach {
                        it.isVisible = false
                        it.clearFontImagesAndRefresh()
                    }
                }.build { player ->
                    PlayerData(
                        player,
                        TerixPlayer.cachedOrigin(player).abilityData[player]
                            .filterIsInstance<KeybindAbility>()
                            .filterNot { ability -> ability.cooldownDuration == Duration.ZERO }
                            .toTypedArray()
                    )
                }

            operator fun get(player: Player): PlayerData = cache[player]
        }
    }
}
