package dev.racci.terix.core.services

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.github.benmanes.caffeine.cache.Caffeine
import com.willfp.eco.core.items.Items
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.builders.ItemBuilderDSL
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.message
import dev.racci.minix.api.extensions.noItalic
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.minix.api.extensions.scheduler
import dev.racci.minix.api.services.DataService
import dev.racci.minix.api.services.DataService.Companion.inject
import dev.racci.minix.api.utils.adventure.PartialComponent.Companion.message
import dev.racci.minix.api.utils.now
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.Terix
import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.data.TerixConfig
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.core.data.Lang
import dev.racci.terix.core.extensions.freeChanges
import dev.racci.terix.core.extensions.originTime
import dev.racci.terix.core.extensions.toVec
import dev.racci.terix.core.utils.RenderablePaginatedTransform
import kotlinx.datetime.Instant
import net.kyori.adventure.extra.kotlin.text
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
import org.incendo.interfaces.core.arguments.ArgumentKey
import org.incendo.interfaces.core.arguments.HashMapInterfaceArguments
import org.incendo.interfaces.core.click.ClickHandler
import org.incendo.interfaces.core.util.Vector2
import org.incendo.interfaces.core.view.InterfaceView
import org.incendo.interfaces.kotlin.arguments
import org.incendo.interfaces.kotlin.paper.GenericClickHandler
import org.incendo.interfaces.kotlin.paper.MutableChestInterfaceBuilder
import org.incendo.interfaces.kotlin.paper.MutableChestPaneView
import org.incendo.interfaces.kotlin.paper.asElement
import org.incendo.interfaces.kotlin.paper.asViewer
import org.incendo.interfaces.kotlin.paper.buildChestInterface
import org.incendo.interfaces.paper.PlayerViewer
import org.incendo.interfaces.paper.element.ItemStackElement
import org.incendo.interfaces.paper.pane.ChestPane
import org.incendo.interfaces.paper.type.ChestInterface
import org.koin.core.component.get
import org.koin.core.component.inject
import java.time.Duration
import kotlin.math.ceil
import kotlin.time.Duration.Companion.seconds

// TODO: Send fixup packet when player clicks an item
// TODO: When clicking on another item after the first the second doesnt flash.
// TODO -> Lime green instead of green wool.
// TODO -> Move buttons down one row.
// TODO -> Add block below each item which shows detailed info about the origin.
@MappedExtension(Terix::class, "GUI Service", [OriginService::class])
public class GUIService(override val plugin: Terix) : Extension<Terix>() {
    private val lang by inject<DataService>().inject<Lang>()
    private val terixConfig by inject<DataService>().inject<TerixConfig>()
    private val originService by inject<OriginServiceImpl>()
    private val playerArgumentKey = ArgumentKey.of("player", Player::class.java)

    private val packetModifierCache = mutableMapOf<Player, Array<Any?>>()
    private val selectedOrigin = Caffeine.newBuilder()
        .removalListener<HumanEntity, Origin> { key, _, _ ->
            packetModifierCache.remove(key)
        }
        .build<HumanEntity, Origin>()

    private val borderItems = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofSeconds(30))
        .build<Material, ItemStackElement<ChestPane>> {
            ItemBuilderDSL.from(it) {
                name = text { }
            }.asElement()
        }

    private val accessingPlayers = mutableSetOf<Player>()

    private val menu by lazy {
        withBaseInterface(originService.registeredOrigins.size) {
            val pageTransformer = createReactivatePagination(rows, originService.getOrigins().values) { element, view -> element.createItem(view) }
            this.addTransform(pageTransformer, 2)
            this.withCloseHandler { _, view -> accessingPlayers.remove(view.viewer().player()); selectedOrigin.invalidate(view.viewer().player()) }
        }
    }

    override suspend fun handleDisable() {
        accessingPlayers.forEach(Player::closeInventory)
    }

    public fun openMenu(player: Player) {
        menu.open(
            player.asViewer(),
            HashMapInterfaceArguments
                .with(playerArgumentKey, player)
                .build()
        )

        accessingPlayers.add(player)
    }

    private fun getButton(
        button: TerixConfig.GUI.GUIItemSlot,
        action: GenericClickHandler<ChestPane> = GenericClickHandler.cancel()
    ): ItemStackElement<ChestPane> {
        val item = Items.lookup(button.display)
        return ItemBuilderDSL.from(item.item.clone()) {
            lore = button.lore.map { it.get() }
        }.asElement(action)
    }

    private fun withBaseInterface(
        elements: Int,
        builder: MutableChestInterfaceBuilder.() -> Unit
    ): ChestInterface = buildChestInterface {
        rows = getPageSize(elements)
        clickHandler = ClickHandler.cancel()

        this.border()
        this.withTransform(1) { view -> view.insertButtons(rows) }
        this.builder()
    }

    private fun getPageSize(elements: Int): Int {
        var rows = 2 // Header and footer
        rows += ceil(minOf(elements, 36).toDouble() / 9.0).toInt() // Allow 1-4 rows of items
        return rows.coerceAtLeast(3) // The above can be 0, so we need to make sure we have at least 3 rows.
    }

    private fun <T : Any> createReactivatePagination(
        rows: Int,
        insertedElements: Collection<T>,
        transformation: (T, InterfaceView<ChestPane, PlayerViewer>) -> ItemStackElement<ChestPane>
    ): RenderablePaginatedTransform<ItemStackElement<ChestPane>, ChestPane, PlayerViewer> {
        val buttons = get<TerixConfig>().gui
        return RenderablePaginatedTransform<ItemStackElement<ChestPane>, ChestPane, PlayerViewer>(
            Vector2.at(1, 1),
            Vector2.at(7, rows - 2)
        ) { insertedElements.map { element -> { view -> transformation(element, view) } } }.apply {
            this.backwardElement(buttons.previousPage.position.toVec(rows)) { transform -> getButton(buttons.previousPage) { ctx -> selectedOrigin.invalidate(ctx.viewer().player()); transform.previousPage() } }
            this.forwardElement(buttons.nextPage.position.toVec(rows)) { transform -> getButton(buttons.nextPage) { ctx -> selectedOrigin.invalidate(ctx.viewer().player()); transform.nextPage() } }
        }
    }

    // TODO: Sometimes glitches out.
    override suspend fun handleEnable() {
        ProtocolLibrary.getProtocolManager().addPacketListener(
            object : PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.SET_SLOT, PacketType.Play.Client.WINDOW_CLICK) {
                override fun onPacketSending(event: PacketEvent) {
                    if (!validPacket(event)) return

                    val cache = packetModifierCache[event.player]!!
                    val nms = event.player.toNMS()
                    event.packet.integers.write(1, nms.containerMenu.incrementStateId())
                    event.packet.integers.write(0, nms.containerMenu.containerId)

                    if (cache[2].castOrThrow()) {
                        event.packet.itemModifier.write(0, cache[1].castOrThrow())
                    } else event.packet.itemModifier.write(0, borderItems[Material.LIME_STAINED_GLASS_PANE].itemStack())

                    if (cache.getOrNull(3) != null) {
                        cache[3] = null
                    } else cache[2] = !cache[2].castOrThrow<Boolean>()
                }

                override fun onPacketReceiving(event: PacketEvent) {
                    if (validPacket(event)) sendPacket(event.player, event.packet.integers.read(2))
                }

                private fun validPacket(event: PacketEvent) = event.player in packetModifierCache && packetModifierCache[event.player]!![0] == event.packet.integers.read(2)
            }
        )
    }

    private fun sendPacket(player: Player, slot: Int) {
        val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SET_SLOT)
        packet.integers.write(2, slot)
        ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet)
    }

    private fun Origin.createItem(view: InterfaceView<ChestPane, PlayerViewer>): ItemStackElement<ChestPane> {
        return ItemBuilderDSL.from(item.material) {
            name = (item.name ?: displayName).noItalic()

            lore = item.loreComponents
            if (requirements.isNotEmpty()) {
                lore = lore + lang.gui.requirementLore.map { it.get() } + requirements.map { (lore, checker) ->
                    val colour = if (checker(view.arguments.get(playerArgumentKey))) {
                        Component.empty().color(NamedTextColor.GREEN)
                    } else Component.empty().color(NamedTextColor.RED)
                    lang.gui.requirementLine["requirement" to { colour.append(lore) }]
                }

                if (StorageService.transaction { TerixPlayer[view.arguments.get(playerArgumentKey)].grants.contains(this@createItem.name) }) {
                    lore = lore + Component.empty() + lang.gui.hasGrant.get()
                }
            }
            lore = lore.map(Component::noItalic)
        }.asElement { ctx ->
            if (selectedOrigin.getIfPresent(ctx.cause().whoClicked) == this@createItem) {
                ctx.cause().whoClicked.shieldSound()
                return@asElement selectedOrigin.invalidate(ctx.cause().whoClicked)
            }

            selectedOrigin.put(ctx.cause().whoClicked, this@createItem)
            packetModifierCache[ctx.cause().whoClicked.castOrThrow()] = arrayOf(ctx.cause().rawSlot, ctx.cause().clickedInventory!!.getItem(ctx.cause().rawSlot), false)
            ctx.cause().whoClicked.playSound(Sound.sound(Key.key("block.beehive.enter"), Sound.Source.MASTER, 1f, 0.8f))

            scheduler {
                if (!packetModifierCache.contains(ctx.cause().whoClicked)) return@scheduler it.cancel()

                sendPacket(ctx.cause().whoClicked.castOrThrow(), ctx.cause().rawSlot)
            }.runTaskTimer(plugin, 0.1.seconds, 1.seconds)
        }
    }

    private fun Instant.remaining(cooldown: kotlin.time.Duration) = (this + cooldown - now()).takeUnless { it.inWholeMilliseconds <= 0 }

    private fun kotlin.time.Duration.format() = toComponents { days, hours, minutes, seconds, _ ->
        StringBuilder().apply {
            if (days > 0) append("$days days")
            if (hours > 0) { appendExtra(false); append("$hours hours") }
            if (minutes > 0) { appendExtra(hours > 0 || days > 0 || seconds == 0); append("${minutes}m") }
            if (days == 0L && hours == 0 && seconds > 0) { appendExtra(true); append(" $seconds seconds") }
        }.toString()
    }

    private fun StringBuilder.appendExtra(last: Boolean) {
        if (isNotEmpty()) append(", ")
        if (last) append("and ")
    }

    private fun MutableChestInterfaceBuilder.border() {
        withTransform(0) { view ->
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

                for (col in nums) view[col, row] = borderItems[mat]
            }
        }
    }

    private fun HumanEntity.shieldSound() {
        playSound(Sound.sound(Key.key("item.shield.break"), Sound.Source.MASTER, 1f, 0.5f))
    }

    private fun MutableChestPaneView.insertButtons(rows: Int) {
        fun vecPos(button: TerixConfig.GUI.GUIItemSlot): Vector2 {
            val (col, row) = button.position.toVec()
            return Vector2.at(col, if (rows < row || row == -1) rows - 1 else row)
        }

        val buttons = get<TerixConfig>().gui

        this[vecPos(buttons.remainingChanges)] = ItemBuilderDSL.from(Items.lookup(buttons.remainingChanges.display).item) {
            val player = arguments.get(playerArgumentKey)
            val free = player.freeChanges
            lore = listOf(
                Component.empty(),
                when {
                    free > 0 -> lang.gui.changeFree["amount" to { free }]
                    player.originTime.remaining(terixConfig.intervalBeforeChange) == null -> lang.gui.changeTime.get()
                    else -> lang.gui.changeTimeCooldown["cooldown" to { player.originTime.remaining(terixConfig.intervalBeforeChange)!!.format() }]
                }
            )
        }.asElement()

        this[vecPos(buttons.info)] = getButton(buttons.info)
        this[vecPos(buttons.cancelSelection)] = getButton(buttons.cancelSelection) { ctx ->
            if (selectedOrigin.getIfPresent(ctx.viewer().player()) == null) return@getButton
            ctx.viewer().player().shieldSound()
            selectedOrigin.invalidate(ctx.viewer().player())
        }
        this[vecPos(buttons.confirmSelection)] = getButton(buttons.confirmSelection) { ctx ->
            val player = ctx.viewer().player()
            val origin = selectedOrigin.getIfPresent(player) ?: return@getButton
            async {
                val bypass = player.freeChanges > 0
                val event = PlayerOriginChangeEvent(player, TerixPlayer.cachedOrigin(player), origin, bypass)

                if (event.callEvent()) {
                    if (bypass) player.freeChanges--
                    sync { player.closeInventory(InventoryCloseEvent.Reason.PLAYER) }
                    player.playSound(Sound.sound(Key.key("block.chest.unlock"), Sound.Source.PLAYER, 1.0f, 1.0f))
                } else {
                    player.shieldSound()

                    when (event.result) {
                        PlayerOriginChangeEvent.Result.ON_COOLDOWN -> lang.origin.onChangeCooldown["cooldown" to { player.originTime.remaining(terixConfig.intervalBeforeChange)!!.format() }] message player
                        PlayerOriginChangeEvent.Result.NO_PERMISSION -> lang.origin.missingRequirement.message(player)
                        PlayerOriginChangeEvent.Result.CURRENT_ORIGIN -> {
                            lang.origin.setSameSelf["origin" to { origin.displayName }] message player
                            selectedOrigin.invalidate(player)
                        }

                        else -> { /* Do Nothing. */
                        }
                    }
                }
            }
        }
    }

    public companion object : ExtensionCompanion<GUIService>()
}
