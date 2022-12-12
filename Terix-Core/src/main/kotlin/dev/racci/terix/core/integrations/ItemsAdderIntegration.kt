package dev.racci.terix.core.integrations

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
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
import dev.racci.terix.api.data.player.TerixPlayer
import dev.racci.terix.api.events.abilities.KeybindAbilityActivateEvent
import dev.racci.terix.api.origins.abilities.keybind.KeybindAbility
import dev.racci.terix.api.origins.origin.OriginValues.AbilityData.PlayerAbilityHolder
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
                FontImageWrapper(id).also(images::add)
            } while (i++ < 50 && images.last().exists())

            if (images.isEmpty()) throw logger.fatal { "No font images found for ability bars!" }

            PlayerData.fontImages = images.filter(FontImageWrapper::exists).reversed().toTypedArray()
        }

        event<KeybindAbilityActivateEvent>(EventPriority.MONITOR, true) {
            val terixPlayer = TerixPlayer[player]
            val index = terixPlayer.origin.abilityData[terixPlayer].indexOf(this.ability)
            PlayerData[terixPlayer].tickAbility(index, this.ability)
        }

//        event<PlayerOriginChangeEvent>(EventPriority.MONITOR) {
//
//        }

        event<PlayerJoinEvent>(EventPriority.MONITOR, true) { PlayerData[TerixPlayer[this.player]] }

        TickService.playerFlow
            .map(TerixPlayer::get)
            .map(PlayerData::get)
            .onEach(PlayerData::tick)
            .launchIn(plugin.scope + plugin.asyncDispatcher)
    }

    override fun filterResource(name: String): Boolean = name.startsWith("contents/")

    public data class PlayerData(
        val playerRef: TerixPlayer,
        val abilities: Array<KeybindAbility>,
        val holderWrapper: PlayerHudsHolderWrapper,
        val hudElements: Array<PlayerCustomHudWrapper>
    ) {
        public fun tick() {
            abilities.forEachIndexed { index, ability -> tickAbility(index, ability) }
        }

        public fun tickAbility(
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

        public companion object {
            internal var fontImages: Array<FontImageWrapper> = emptyArray()
            internal val cache: Cache<Player, PlayerData> = Caffeine
                .newBuilder()
                .weakKeys()
                .build()

            internal fun filteredAbilities(abilityHolder: PlayerAbilityHolder): Array<KeybindAbility> {
                return abilityHolder.abilities.filterIsInstance<KeybindAbility>()
                    .filterNot { ability -> ability.cooldownDuration == Duration.ZERO }
                    .toTypedArray()
            }

            internal fun generateHudElements(
                holder: PlayerHudsHolderWrapper,
                initialShownSize: Int
            ) = Array(fontImages.size) { index ->
                PlayerCustomHudWrapper(
                    holder,
                    "terix:ability_bar_$index"
                ).apply {
                    println("Creating element $index of ${fontImages.lastIndex}, shown size: $initialShownSize")
                    val imageWidth = fontImages.first().width
                    val imageBasedOffset = imageWidth * index
                    this.offsetX += if (index == 0) 0 else imageBasedOffset + ((imageWidth / 2) * index)
                    if (index < initialShownSize) {
                        println("Adding font image to element $index")
                        this.floatValue = fontImages.size.toFloat()
                        this.addFontImage(fontImages.first())
                        this.isVisible = true
                    }
                }
            }

            internal fun create(player: TerixPlayer): PlayerData {
                val wrapper = PlayerHudsHolderWrapper(player.backingPlayer)
                val abilities = filteredAbilities(player.origin.abilityData[player])
                return PlayerData(
                    player,
                    abilities,
                    wrapper,
                    generateHudElements(wrapper, abilities.size)
                )
            }

            public operator fun get(player: TerixPlayer): PlayerData = cache.get(player.backingPlayer) { _ ->
                create(player).also { data ->
                    data.holderWrapper.recalculateOffsets()
                    data.holderWrapper.sendUpdate()
                }
            }
        }
    }
}
