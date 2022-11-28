package dev.racci.terix.api.data.player

import com.github.benmanes.caffeine.cache.Caffeine
import dev.racci.minix.api.extensions.onlinePlayer
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.services.StorageService
import kotlinx.datetime.Instant
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.inventory.Book
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.sound.SoundStop
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.TitlePart
import net.minecraft.server.level.ServerPlayer
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer
import org.bukkit.entity.Player
import org.jetbrains.exposed.dao.DaoEntityID
import org.jetbrains.exposed.dao.EntityChangeType
import org.jetbrains.exposed.dao.EntityHook
import org.jetbrains.exposed.dao.toEntity
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.util.UUID

// TODO -> Maybe mark all as immutable and make a new class when origin changes?
// TODO -> Passing this player instance can cause issues as bukkit just casts to CraftPlayer.
public class TerixPlayer private constructor(
    private val id: DaoEntityID<UUID>,
    public val backingPlayer: CraftPlayer = onlinePlayer(id.value) as? CraftPlayer ?: error("Player with UUID $id is not online"),
    snapshotEntity: TerixPlayerEntity
) : Player by backingPlayer, OverrideFixer, TerixPlayerEntity {
    public val ticks: TickCache = TickCache()

    /** If called from within a transaction gets the entity from the transaction, otherwise gets returns the snapshot state. */
    public var databaseEntity: TerixPlayerEntity = snapshotEntity
        internal set
        get() = if (TransactionManager.currentOrNull() != null) {
            TerixPlayerEntityHolder[id]
        } else field

    public val handle: ServerPlayer get() = backingPlayer.handle

    override var lastChosenTime: Instant
        get() = databaseEntity.lastChosenTime
        set(value) = ensureTransaction { databaseEntity.lastChosenTime = value }

    override var freeChanges: Int
        get() = databaseEntity.freeChanges
        set(value) = ensureTransaction { databaseEntity.freeChanges = value }
    override val grants: MutableSet<String>
        get() = databaseEntity.grants

    override var origin: Origin
        get() = databaseEntity.origin
        set(value) = ensureTransaction { databaseEntity.origin = value }

    private inline fun ensureTransaction(f: () -> Unit) {
        if (TransactionManager.currentOrNull() == null) error("Unable to perform set operation on snapshot entity.")
        f()
    }

    public companion object {
        private val playerCache = Caffeine.newBuilder().weakKeys()
            .build<Player, TerixPlayer>()

        public operator fun get(player: Player): TerixPlayer {
            if (player is TerixPlayer) return player
            if (player !is CraftPlayer) error("Player $player is not a CraftPlayer")

            return playerCache.get(player) {
                TerixPlayer(
                    DaoEntityID(player.uniqueId, TerixUser),
                    player,
                    StorageService.transaction { TerixPlayerEntityHolder[player.uniqueId] }
                )
            }
        }

        public operator fun get(uuid: UUID): TerixPlayer {
            return playerCache.asMap().entries.find { it.key.uniqueId == uuid }?.value
                ?: onlinePlayer(uuid)?.let { return get(it) }
                ?: error("Player with UUID $uuid is not online and not in cache.")
        }

        init {
            EntityHook.subscribe { change ->
                println("EntityHook: $change")
                if (change.changeType != EntityChangeType.Updated) return@subscribe println("Not updated")
                val newEntity = change.toEntity<UUID, TerixPlayerEntityHolder>() ?: return@subscribe println("Not a TerixPlayerEntityHolder")
                get(newEntity.id.value).databaseEntity = newEntity
            }
        }

        @JvmName("cachedOriginNotNull")
        @Suppress("INAPPLICABLE_JVM_NAME")
        @Deprecated("Use class methods instead", ReplaceWith("TerixPlayer[player].origin"))
        public fun cachedOrigin(player: Player): Origin = this[player].origin

        @JvmName("cachedOriginNullable")
        @Suppress("INAPPLICABLE_JVM_NAME")
        @Deprecated("Use class methods instead", ReplaceWith("player?.let { TerixPlayer[player].origin }"))
        public fun cachedOrigin(player: Player?): Origin? = when (player) {
            null -> null
            else -> this[player].origin
        }
    }

    override fun sendActionBar(message: Component) {
        backingPlayer.sendActionBar(message)
    }

    override fun sendPlayerListHeader(header: Component) {
        backingPlayer.sendPlayerListHeader(header)
    }

    override fun sendPlayerListFooter(footer: Component) {
        backingPlayer.sendPlayerListFooter(footer)
    }

    override fun sendPlayerListHeaderAndFooter(header: Component, footer: Component) {
        backingPlayer.sendPlayerListHeaderAndFooter(header, footer)
    }

    override fun showTitle(title: Title) {
        backingPlayer.showTitle(title)
    }

    override fun <T : Any> sendTitlePart(part: TitlePart<T>, value: T) {
        backingPlayer.sendTitlePart(part, value)
    }

    override fun clearTitle() {
        backingPlayer.clearTitle()
    }

    override fun showBossBar(bar: BossBar) {
        backingPlayer.showBossBar(bar)
    }

    override fun hideBossBar(bar: BossBar) {
        backingPlayer.hideBossBar(bar)
    }

    override fun playSound(sound: Sound) {
        backingPlayer.playSound(sound)
    }

    override fun playSound(sound: Sound, x: Double, y: Double, z: Double) {
        backingPlayer.playSound(sound, x, y, z)
    }

    override fun playSound(sound: Sound, emitter: Sound.Emitter) {
        backingPlayer.playSound(sound, emitter)
    }

    override fun stopSound(stop: SoundStop) {
        backingPlayer.stopSound(stop)
    }

    override fun openBook(book: Book) {
        backingPlayer.openBook(book)
    }
}
