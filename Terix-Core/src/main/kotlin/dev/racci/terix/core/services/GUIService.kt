package dev.racci.terix.core.services

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.pane.Pane
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.builders.ItemBuilderDSL
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.message
import dev.racci.minix.api.extensions.parse
import dev.racci.minix.api.extensions.scheduler
import dev.racci.minix.api.services.DataService
import dev.racci.minix.api.services.DataService.Companion.inject
import dev.racci.minix.api.utils.now
import dev.racci.minix.api.utils.unsafeCast
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.Terix
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.core.data.Config
import dev.racci.terix.core.data.Lang
import dev.racci.terix.core.extension.asGuiItem
import dev.racci.terix.core.extension.dsl
import dev.racci.terix.core.extension.origin
import dev.racci.terix.core.extension.originTime
import dev.racci.terix.core.extension.usedChoices
import dev.racci.terix.core.origins.AngelOrigin
import dev.racci.terix.core.origins.AxolotlOrigin
import dev.racci.terix.core.origins.BeeOrigin
import dev.racci.terix.core.origins.BlizzOrigin
import dev.racci.terix.core.origins.DragonOrigin
import dev.racci.terix.core.origins.FairyOrigin
import dev.racci.terix.core.origins.HumanOrigin
import dev.racci.terix.core.origins.MerlingOrigin
import dev.racci.terix.core.origins.SlimeOrigin
import dev.racci.terix.core.origins.VampireOrigin
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.until
import net.kyori.adventure.extra.kotlin.text
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.Material
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.inventory.InventoryCloseEvent
import org.koin.core.component.inject
import java.time.Duration
import kotlin.math.ceil
import kotlin.time.Duration.Companion.seconds

@MappedExtension(Terix::class, "GUI Service", [OriginService::class, HookService::class])
class GUIService(override val plugin: Terix) : Extension<Terix>() {
    private val lang by inject<DataService>().inject<Lang>()
    private val config by inject<DataService>().inject<Config>()
    private val hookService by inject<HookService>()
    private val originService by inject<OriginServiceImpl>()

    private val packetModifierCache = mutableMapOf<Player, Array<Any?>>()
    private val selectedOrigin = Caffeine.newBuilder()
        .removalListener<HumanEntity, AbstractOrigin> { key, _, _ ->
            packetModifierCache.remove(key)
        }
        .build<HumanEntity, AbstractOrigin>()

    private val borderItems = lazy {
        Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofSeconds(30))
            .build<Material, GuiItem> {
                ItemBuilderDSL.from(it) {
                    name = text { }
                }.asGuiItem()
            }
    }

    val baseGui = lazy(::baseGUI)

    private fun baseGUI(): ChestGui {
        val guiRows = ceil(originService.registeredOrigins.size.toDouble() / 9).coerceAtLeast(1.0).toInt() + 4
        val pane = StaticPane(1, 1, 7, guiRows).apply {

            addItem(originService.getOrigin<AngelOrigin>().createItem(), 0, 0)
            addItem(originService.getOrigin<AxolotlOrigin>().createItem(), 1, 0)
            addItem(originService.getOrigin<BeeOrigin>().createItem(), 2, 0)
            addItem(originService.getOrigin<BlizzOrigin>().createItem(), 3, 0)
            addItem(originService.getOrigin<DragonOrigin>().createItem(), 4, 0)
            addItem(originService.getOrigin<FairyOrigin>().createItem(), 5, 0)
            addItem(originService.getOrigin<HumanOrigin>().createItem(), 6, 0)
            addItem(originService.getOrigin<MerlingOrigin>().createItem(), 0, 1)
            addItem(originService.getOrigin<SlimeOrigin>().createItem(), 1, 1)
            addItem(originService.getOrigin<VampireOrigin>().createItem(), 2, 1)

            // TODO: Fix this so its not manually placed
            /* var x = 0
            var y = 0
            originService.registry.values.forEachIndexed { index, origin ->
                log.debug { "Adding origin $origin to gui at x: $x, y: $y from an index of $index" }
                addItem(origin.createItem(), x, y)
                if (index / 7 < 1) {
                    x++
                } else {
                    x = 0
                    y++
                }
            } */
        }

        return ChestGui(guiRows, "Origin GUI").apply {
            fillBorder.value[rows].let(panes::add)
            panes += pane
            panes += StaticPane(3, rows - 2, 3, 1).apply {
                addItem(cancelButton.value, 0, 0)
                addItem(closeButton.value, 1, 0)
                addItem(confirmButton.value, 2, 0)
            }
            setOnGlobalClick(Cancellable::cancel)
            setOnClose { selectedOrigin.invalidate(it.player) }
        }
    }

    override suspend fun handleEnable() {
        hookService.protocolManager.addPacketListener(
            object : PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.SET_SLOT, PacketType.Play.Client.WINDOW_CLICK) {
                override fun onPacketSending(event: PacketEvent) {

                    if (!validPacket(event)) return

                    val cache = packetModifierCache[event.player]!!
                    val nms = event.player.toNMS()
                    event.packet.integers.write(1, nms.containerMenu.incrementStateId())
                    event.packet.integers.write(0, nms.containerMenu.containerId)

                    if (cache[2].unsafeCast()) {
                        event.packet.itemModifier.write(0, cache[1].unsafeCast())
                    } else event.packet.itemModifier.write(0, borderItems.value[Material.LIME_STAINED_GLASS_PANE].item)

                    if (cache.getOrNull(3) != null) {
                        cache[3] = null
                    } else cache[2] = !cache[2].unsafeCast<Boolean>()
                }

                override fun onPacketReceiving(event: PacketEvent) {
                    if (validPacket(event)) sendPacket(event.player, event.packet.integers.read(2))
                }

                private fun validPacket(event: PacketEvent) =
                    event.player in packetModifierCache && packetModifierCache[event.player]!![0] == event.packet.integers.read(2)
            }
        )
    }

    private fun sendPacket(player: Player, slot: Int) {
        val packet = hookService.protocolManager.createPacket(PacketType.Play.Server.SET_SLOT)
        packet.integers.write(2, slot)
        hookService.protocolManager.sendServerPacket(player, packet)
    }

    private fun AbstractOrigin.createItem(): GuiItem {
        return ItemBuilderDSL.from(item.material) {
            name = item.name
            lore = item.loreComponent
        }.asGuiItem {
            if (selectedOrigin.getIfPresent(whoClicked) == this@createItem) {
                whoClicked.shieldSound()
                return@asGuiItem selectedOrigin.invalidate(whoClicked)
            }

            selectedOrigin.put(whoClicked, this@createItem)
            packetModifierCache[whoClicked.unsafeCast()] = arrayOf(rawSlot, this.clickedInventory!!.getItem(rawSlot), false)
            whoClicked.playSound(Sound.sound(Key.key("block.beehive.enter"), Sound.Source.MASTER, 1f, 0.8f))

            scheduler {
                if (!packetModifierCache.contains(whoClicked)) return@scheduler it.cancel()

                sendPacket(whoClicked.unsafeCast(), rawSlot)
            }.runTaskTimer(plugin, 0.1.seconds, 1.seconds)
        }
    }

    private val confirmButton = lazy {
        ItemBuilderDSL.from(Material.GREEN_WOOL) {
            name = "<i:false><green>Confirm Selection".parse()
            lore("<i:false><white><bold>»</bold> <aqua>Click</aqua> <bold>»</bold>to confirm your selection.".parse())
        }.asGuiItem {
            val player = whoClicked as? Player ?: return@asGuiItem
            val origin = selectedOrigin.getIfPresent(player) ?: return@asGuiItem
            async {
                if (player.origin() == origin) {
                    player.shieldSound()
                    lang.origin.setSameSelf["origin" to { origin.displayName }] message player
                    return@async selectedOrigin.invalidate(player)
                }

                val bypass = config.freeChanges > 0 && player.usedChoices < config.freeChanges
                if (PlayerOriginChangeEvent(player, player.origin(), origin, bypass).callEvent()) {
                    if (bypass) player.usedChoices++
                    sync { player.closeInventory(InventoryCloseEvent.Reason.PLAYER) }
                    player.playSound(Sound.sound(Key.key("block.chest.unlock"), Sound.Source.PLAYER, 1.0f, 1.0f))
                } else {
                    player.shieldSound()
                    player.sendMessage("You must wait ${player.originTime.until(now(), DateTimeUnit.MINUTE)} minutes before changing origins.")
                }
            }
        }
    }

    private val closeButton = lazy {
        ItemBuilderDSL.from(Material.RED_STAINED_GLASS_PANE) {
            name = "<i:false><red>Close Menu".parse()
            lore("<i:false><white><bold>»</bold> <aqua>Click</aqua> <bold>»</bold>to close this menu.".parse())
        }.asGuiItem {
            whoClicked.closeInventory(InventoryCloseEvent.Reason.PLAYER)
            whoClicked.playSound(Sound.sound(Key.key("item.book.page_turn"), Sound.Source.PLAYER, 1.0f, 1.0f))
        }
    }

    private val cancelButton = lazy {
        ItemBuilderDSL.from(Material.RED_WOOL) {
            name = "<i:false><red>Cancel selection".parse()
            lore("<i:false><white><bold>»</bold> <aqua>Click</aqua> <bold>«</bold> <yellow>to cancel your selection".parse())
        }.asGuiItem {
            if (selectedOrigin.getIfPresent(whoClicked) == null) return@asGuiItem
            whoClicked.shieldSound()
            selectedOrigin.invalidate(whoClicked)
        }
    }

    private val fillBorder = lazy {
        Caffeine.newBuilder()
            .build { rows: Int ->
                if (rows < 2) return@build null // We don't want to fill it up completely

                StaticPane(0, 0, 9, rows, Pane.Priority.LOWEST).dsl {
                    for (row in rows downTo 0) {
                        val nums = when (row) {
                            rows - 1, 0 -> arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 8)
                            else -> arrayOf(0, 8)
                        }
                        val mat = when (row) {
                            rows - 1 -> Material.PINK_STAINED_GLASS_PANE
                            0 -> Material.CYAN_STAINED_GLASS_PANE
                            1 -> Material.LIGHT_BLUE_STAINED_GLASS_PANE
                            2 -> Material.BLUE_STAINED_GLASS_PANE
                            3 -> Material.PURPLE_STAINED_GLASS_PANE
                            else -> Material.MAGENTA_STAINED_GLASS_PANE
                        }
                        for (col in nums) addItem(borderItems.value[mat], col, row)
                    }
                }
            }
    }

    private fun HumanEntity.shieldSound() {
        playSound(Sound.sound(Key.key("item.shield.break"), Sound.Source.MASTER, 1f, 0.5f))
    }

    companion object : ExtensionCompanion<GUIService>()
}
