package dev.racci.terix.core.services

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.pane.Pane
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.builders.ItemBuilderDSL
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.displayName
import dev.racci.minix.api.extensions.editItemMeta
import dev.racci.minix.api.extensions.parse
import dev.racci.minix.api.extensions.scheduler
import dev.racci.minix.api.scheduler.CoroutineTask
import dev.racci.minix.api.utils.kotlin.ifTrue
import dev.racci.minix.api.utils.now
import dev.racci.terix.api.Terix
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.core.extension.asGuiItem
import dev.racci.terix.core.extension.dsl
import dev.racci.terix.core.extension.origin
import dev.racci.terix.core.extension.originTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.until
import net.kyori.adventure.extra.kotlin.text
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.Material
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.meta.ItemMeta
import org.koin.core.component.inject
import java.time.Duration
import kotlin.math.ceil
import kotlin.time.Duration.Companion.seconds

@MappedExtension(Terix::class, "GUI Service", [OriginService::class])
class GUIService(override val plugin: Terix) : Extension<Terix>() {
    private val originService by inject<OriginService>()

    private val selectedOrigin = Caffeine.newBuilder()
        .removalListener<HumanEntity, Pair<AbstractOrigin, CoroutineTask>> { key, value, _ ->
            log.debug { "Cancelling menu task for ${key?.name}" }
            runBlocking { value?.second?.cancel() }
        }
        .build<HumanEntity, Pair<AbstractOrigin, CoroutineTask>>()

    private val borderItems = lazy {
        Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofSeconds(30))
            .build<Material, GuiItem> {
                ItemBuilderDSL.from(it) {
                    name = text { }
                }.asGuiItem {
                    whoClicked.playSound(Sound.sound(Key.key("block.chest.locked"), Sound.Source.PLAYER, 1.0f, 1.0f))
                }
            }
    }

    val baseGui = lazy(::baseGUI)

    private fun baseGUI(): ChestGui {
        val guiRows = ceil(originService.registeredOrigins.size.toDouble() / 9).coerceAtLeast(1.0).toInt()
        val pane = StaticPane(1, 1, 7, guiRows + 3).apply {
            var x = 0
            var y = 0
            originService.registry.values.forEachIndexed { index, origin ->
                addItem(origin.createItem(this, x, y), x, y)
                if (index / 7 < 1) {
                    x++
                } else {
                    x = 0
                    y++
                }
            }
        }

        return ChestGui(guiRows, "Origin GUI").apply {
            fillBorder.value[rows]?.let(panes::add)
            panes += pane
            panes += StaticPane(3, rows - 2, 3, 1).apply {
                addItem(cancelButton.value, 0, 0)
                addItem(closeButton.value, 1, 0)
                addItem(confirmButton.value, 2, 0)
            }
            setOnGlobalClick() { it.cancel() }
            setOnClose { selectedOrigin.invalidate(it.player) }
        }
    }

    private fun AbstractOrigin.createItem(
        pane: StaticPane,
        x: Int,
        y: Int
    ): GuiItem {
        val itemStack = ItemBuilderDSL.from(itemMaterial) {
            name = itemName
            lore = itemLore
        }
        val guiItem = itemStack.asGuiItem()
        return guiItem.apply {
            setAction { event ->
                if (selectedOrigin.getIfPresent(event.whoClicked)?.first == this@createItem) {
                    selectedOrigin.invalidate(event.whoClicked)
                    return@setAction
                }
                val task = scheduler {
                    var boolean = true
                    while (selectedOrigin.getIfPresent(event.whoClicked) == this@createItem) {
                        when (boolean) {
                            true -> pane.addItem(borderItems.value[Material.LIME_STAINED_GLASS_PANE], x, y)
                            false -> pane.addItem(guiItem, x, y)
                        }
                        boolean = !boolean
                        delay(1.5.seconds)
                    }
                }.runAsyncTask(plugin)
                selectedOrigin.put(event.whoClicked, this@createItem to task)
            }
        }
    }

    private val confirmButton = lazy {
        borderItems.value[Material.LIME_CONCRETE].apply {
            item.editItemMeta<ItemMeta> {
                displayName("<green>Confirm Selection".parse())
                lore(listOf("<white><bold>»</bold> <aqua>Click</aqua> <bold>»</bold>to confirm your selection.").parse())
            }
            setAction {
                val player = it.whoClicked as? Player ?: return@setAction
                val origin = selectedOrigin.getIfPresent(player)?.first ?: return@setAction
                if (player.originTime.until(now(), DateTimeUnit.MINUTE) > 360) {
                    {
                        PlayerOriginChangeEvent(player, player.origin(), origin).callEvent().ifTrue {
                            log.debug { "Changing origin for ${player.name} from ${player.origin().name} to ${origin.name}" }
                            player.originTime = now()
                            player.closeInventory(InventoryCloseEvent.Reason.PLAYER)
                            player.playSound(Sound.sound(Key.key("block.chest.unlock"), Sound.Source.PLAYER, 1.0f, 1.0f))
                        }
                    }.async()
                }
            }
        }
    }

    private val closeButton = lazy {
        borderItems.value[Material.RED_STAINED_GLASS_PANE].apply {
            item.displayName("<red>Close Menu".parse())
            item.lore(listOf("<white><bold>»</bold> <aqua>Click</aqua> <bold>»</bold>to close this menu".parse()))
            setAction { event ->
                event.whoClicked.closeInventory(InventoryCloseEvent.Reason.PLAYER)
                event.whoClicked.playSound(Sound.sound(Key.key("item.book.page_turn"), Sound.Source.PLAYER, 1.0f, 1.0f))
            }
        }
    }

    private val cancelButton = lazy {
        borderItems.value[Material.RED_WOOL].apply {
            item.editItemMeta<ItemMeta> {
                displayName("<red>Cancel selection".parse())
                lore(listOf("<white><bold>»</bold> <aqua>Click</aqua> <bold>«</bold> <yellow>to cancel your selection".parse()))
            }
            setAction { selectedOrigin.invalidate(it.whoClicked) }
        }
    }

    private val fillBorder = lazy {
        Caffeine.newBuilder()
            .build { rows: Int ->
                if (rows < 2) return@build null // We don't want to fill it up completely

                StaticPane(0, 0, 9, rows, Pane.Priority.LOWEST).dsl {
                    for (row in rows downTo 0) {
                        val nums = when (row) {
                            rows, 0 -> arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
                            else -> arrayOf(0, 9)
                        }
                        val mat = when (row) {
                            rows -> Material.PINK_STAINED_GLASS_PANE
                            0 -> Material.CYAN_STAINED_GLASS_PANE
                            1 -> Material.LIGHT_BLUE_STAINED_GLASS_PANE
                            2 -> Material.BLUE_STAINED_GLASS_PANE
                            3 -> Material.PURPLE_STAINED_GLASS_PANE
                            else -> Material.MAGENTA_STAINED_GLASS_PANE
                        }
                        for (col in nums) addItem(borderItems.value[mat], row, col)
                    }
                }
            }
    }
}
