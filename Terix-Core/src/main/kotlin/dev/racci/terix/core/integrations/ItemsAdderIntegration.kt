package dev.racci.terix.core.integrations

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.github.benmanes.caffeine.cache.RemovalCause
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent
import dev.lone.itemsadder.api.FontImages.FontImageWrapper
import dev.lone.itemsadder.api.FontImages.PlayerCustomHudWrapper
import dev.lone.itemsadder.api.FontImages.PlayerHudsHolderWrapper
import dev.racci.minix.api.annotations.MappedIntegration
import dev.racci.minix.api.coroutine.asyncDispatcher
import dev.racci.minix.api.coroutine.scope
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.scheduler
import dev.racci.minix.api.extensions.server
import dev.racci.minix.api.integrations.Integration
import dev.racci.minix.api.plugin.MinixPlugin
import dev.racci.minix.api.utils.getKoin
import dev.racci.terix.api.Terix
import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.events.abilities.KeybindAbilityActivateEvent
import dev.racci.terix.api.origins.abilities.keybind.KeybindAbility
import dev.racci.terix.api.services.TickService
import dev.racci.terix.core.TerixImpl
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import java.util.zip.ZipInputStream
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.nanoseconds

@MappedIntegration(
    "ItemsAdder",
    Terix::class
)
public class ItemsAdderIntegration(override val plugin: MinixPlugin) : Integration {
    override suspend fun handleLoad() {
        extractDefaultAssets()
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

    private fun extractDefaultAssets() {
        val src = TerixImpl::class.java.protectionDomain.codeSource
        val jarLoc = src.location
        val itemsAdderFolder = server.pluginsFolder.resolve("ItemsAdder")
        var needsZip = false

        runCatching {
            logger.info { "Extracting default assets..." }
            ZipInputStream(jarLoc.openStream()).use { stream ->
                while (true) {
                    val entry = stream.nextEntry ?: break
                    val name = entry.name

                    if (entry.isDirectory || !name.startsWith("contents/")) continue

                    val dest = itemsAdderFolder.resolve(name)
                    if (!dest.exists()) {
                        dest.parentFile.mkdirs()
                        dest.createNewFile()
                        logger.debug { "Extracting $name" }
                        plugin.getResource(name)!!.use { input -> dest.outputStream().use(input::copyTo) }
                        needsZip = true
                    }
                }
            }
            logger.info { "Finished extracting default assets." }
        }.onFailure { err -> logger.error(err) { "Failed to extract default assets." } }

        if (needsZip) logger.warn { "Default assets have been extracted. Please run /iazip to finish the setup." }
    }

    private data class PlayerData(private val playerRef: Player) {
        private val holderWrapper = PlayerHudsHolderWrapper(playerRef)
        private val cooldownElement = Array(TerixPlayer.cachedOrigin(playerRef).abilityData[playerRef].size) { index ->
            PlayerCustomHudWrapper(holderWrapper, "terix:ability_bar_$index").apply {
                this.offsetX += 30 * index
                this.isVisible = true
                this.addFontImage(fontImages.last())
            }
        }

        fun tick() {
            TerixPlayer.cachedOrigin(playerRef).abilityData[playerRef]
                .filterIsInstance<KeybindAbility>()
                .forEachIndexed { index, ability -> tickAbility(index, ability) }
        }

        fun tickAbility(
            index: Int,
            ability: KeybindAbility
        ) {
            if (ability.cooldown.expired()) return

            val element = cooldownElement[index]
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

        companion object {
            internal var fontImages: Array<FontImageWrapper> = emptyArray()
            internal val cache: LoadingCache<Player, PlayerData> = Caffeine.newBuilder().weakKeys()
                .evictionListener<Player, PlayerData> { _, value, cause ->
                    when (cause) {
                        RemovalCause.REPLACED -> scheduleCleanup(value!!.playerRef, value.cooldownElement)
                        else -> value!!.cooldownElement.forEach { it.isVisible = false }
                    }
                }.build(::PlayerData)

            operator fun get(player: Player): PlayerData = cache[player]

            private fun scheduleCleanup(
                player: Player,
                elements: Array<PlayerCustomHudWrapper>
            ) {
                scheduler {
                    cache[player].cooldownElement.filter { it !in elements }.forEach { it.isVisible = false }
                }.runAsyncTaskLater(getKoin().get<Terix>(), 1.nanoseconds)
            }
        }
    }
}
