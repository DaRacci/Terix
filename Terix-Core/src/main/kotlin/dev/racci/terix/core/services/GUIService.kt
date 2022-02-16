package dev.racci.terix.core.services

import com.destroystokyo.paper.MaterialTags
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.pane.Pane
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import dev.racci.minix.api.builders.ItemBuilderDSL
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.utils.kotlin.and
import dev.racci.minix.api.utils.kotlin.ifTrue
import dev.racci.terix.api.Terix
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.core.extension.OutlinePane
import dev.racci.terix.core.extension.asGuiItem
import dev.racci.terix.core.extension.borderMask
import dev.racci.terix.core.extension.dsl
import dev.racci.terix.core.extension.origin
import dev.racci.terix.core.extension.repeat
import net.kyori.adventure.extra.kotlin.text
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
import org.koin.core.component.inject

class GUIService(override val plugin: Terix) : Extension<Terix>() {

    override val name = "GUI Service"
    override val dependencies get() = OriginService::class and LangService::class

    private val originService by inject<OriginService>()
    private val langService by inject<LangService>()

    private val borderItems = lazy {
        mutableMapOf<Material, Lazy<GuiItem>>().apply {
            for (mat in MaterialTags.STAINED_GLASS_PANES.values) {
                this[mat] = lazy {
                    ItemBuilderDSL.from(mat) {
                        name = text { }
                    }.asGuiItem {
                        whoClicked.playSound(Sound.sound(Key.key("block.chest.locked"), Sound.Source.PLAYER, 1.0f, 1.0f))
                        cancel()
                    }
                }
            }
        }
    }

    private val confirmGuis = lazy { mutableMapOf<AbstractOrigin, ChestGui>() }
    val baseGui = lazy { baseGUI() }

    override suspend fun handleEnable() {
    }

    private fun baseGUI(): ChestGui {
        val pane = StaticPane(7, 5).dsl {
            var x = 0
            var y = 0
            originService.registry.values.forEachIndexed { index, origin ->
                addItem(origin.createItem(), x, y)
                if (index % 7 == 0) {
                    x++
                } else {
                    x = 0
                    y++
                }
            }
        }

        return ChestGui(6, "Origin GUI").apply {
            fillBorder.value[rows]?.let(panes::add)
            panes += pane
//            setOnGlobalDrag { it.cancel() }
//            setOnBottomClick { it.cancel() }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun AbstractOrigin.createItem(): GuiItem =
        ItemBuilderDSL.from(itemMaterial) {
            name = itemName
            lore = itemLore
        }.asGuiItem {
            confirmGuis.value[this@createItem] = ChestGui(1, "Confirm Selection of $name.").dsl {
                StaticPane(9, 1).dsl {
                    confirmButton.value[this@createItem].apply { for (i in 0 until 4) addItem(this, i, 0) }
                    for (i in 5 until 9) { addItem(cancelButton.value, i, 0) }
                }
            }
        }

    private val confirmButton = lazy {
        Caffeine.newBuilder()
            .build { origin: AbstractOrigin ->
                borderItems.value[Material.GREEN_STAINED_GLASS_PANE]!!.value.apply {
                    setAction {
                        it.cancel()
                        val player = it.whoClicked as? Player ?: return@setAction
                        if (true) { // TODO: Check if player is able to pick new origin
                            PlayerOriginChangeEvent(player, player.origin(), origin).callEvent().ifTrue {
                                player.closeInventory(InventoryCloseEvent.Reason.PLAYER)
                                player.playSound(Sound.sound(Key.key("block.chest.unlock"), Sound.Source.PLAYER, 1.0f, 1.0f))
                            }
                        }
                    }
                }
            }
    }

    private val cancelButton = lazy {
        borderItems.value[Material.RED_STAINED_GLASS_PANE]!!.value.apply {
            setAction {
                it.cancel()
                it.whoClicked.closeInventory(InventoryCloseEvent.Reason.PLAYER)
                it.whoClicked.playSound(Sound.sound(Key.key("item.book.page_turn"), Sound.Source.PLAYER, 1.0f, 1.0f))
                baseGui.value.show(it.whoClicked)
            }
        }
    }

    private val fillBorder = lazy {
        Caffeine.newBuilder()
            .build { rows: Int ->
                if (rows < 2) return@build null // We don't want to fill it up completely

                OutlinePane(0, 0, 9, rows, Pane.Priority.LOWEST) {
                    items += borderItems.value[Material.MAGENTA_STAINED_GLASS_PANE]!!.value
                    items += borderItems.value[Material.PURPLE_STAINED_GLASS_PANE]!!.value
                    repeat = true
                    borderMask()
                }
            }
    }
}
