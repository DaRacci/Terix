package dev.racci.terix.api.data.placement

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.toOption
import com.github.benmanes.caffeine.cache.Caffeine
import dev.racci.minix.api.coroutine.minecraftDispatcher
import dev.racci.minix.api.coroutine.scope
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.flow.eventFlow
import dev.racci.minix.api.utils.getKoin
import dev.racci.minix.api.utils.now
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.Terix
import dev.racci.terix.api.data.player.TerixPlayer
import dev.racci.terix.api.extensions.Lambda
import dev.racci.terix.api.origins.abilities.Ability
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import net.minecraft.core.BlockPos
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.craftbukkit.v1_19_R1.block.CraftBlock
import org.bukkit.craftbukkit.v1_19_R1.block.data.CraftBlockData
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.metadata.FixedMetadataValue
import kotlin.time.Duration

public typealias PosPredicate = (pos: Location) -> Boolean

public data class TemporaryPlacement internal constructor(
    public val abilityRef: Ability,
    public val replacementData: BlockData,
    public val removeAfter: Duration = Duration.INFINITE,
    public val commitCallback: Option<Lambda> = None,
    public val removalCallback: Option<Lambda> = None,
    public val removablePredicate: Option<PosPredicate> = None,
    public val expeditedPredicate: Option<PosPredicate> = None
) {

    public suspend fun commit(location: Location): TemporaryPlacement {
        placementChannel.send(PlacementCommit(location, this))
        return this
    }

    internal fun commitInternal(block: Block): FulfilledPlacementCommit {
        val commit = FulfilledPlacementCommit(this, block.getState(true), now() + removeAfter)
        val nmsWorld = block.world.toNMS()
        val nmsState = (replacementData as CraftBlockData).state
        val nmsPos = BlockPos(block.x, block.y, block.z)
        val nmsOldState = (block as CraftBlock).nms
        nmsWorld.setBlock(nmsPos, nmsState, 2 or 16 or 128 or 1024) // NOTIFY | NO_OBSERVER | NO_LIGHTING_UPDATE | NO_PLACE (custom)
        nmsWorld.minecraftWorld.sendBlockUpdated(nmsPos, nmsOldState, nmsState, 3)
        block.setMetadata(META_KEY, metaValueCache.get(abilityRef.abilityPlayer))
        commitCallback.tap(Lambda::invoke)

        return commit
    }

    internal fun undoCommitInternal(commit: FulfilledPlacementCommit) {
        commit.capturedState.block.setBlockData(commit.capturedState.blockData, false)
        removalCallback.tap(Lambda::invoke)
    }

    private fun changedOrigin(): Boolean = abilityRef.linkedOrigin !== TerixPlayer[abilityRef.abilityPlayer].origin

    private fun changedWorld(commit: FulfilledPlacementCommit): Boolean = commit.capturedState.world != abilityRef.abilityPlayer.world

    @OptIn(ExperimentalCoroutinesApi::class)
    public companion object {
        private const val META_KEY = "replacement-owner"
        private val metaValueCache = Caffeine.newBuilder().weakKeys().build<Player, FixedMetadataValue>() { player -> FixedMetadataValue(getKoin().get<Terix>(), player.uniqueId.toString()) }
        private val placementChannel = Channel<PlacementCommit>()

        init {
            val terix = getKoin().get<Terix>()
            val mainScope = terix.scope + terix.minecraftDispatcher

            terix.eventFlow<BlockBreakEvent>(priority = EventPriority.LOWEST)
                .filter { event -> event.block.hasMetadata(META_KEY) }
                .filterNot { event -> event.block.getMetadata(META_KEY).all { meta -> meta.asString() == event.player.uniqueId.toString() } }
                .onEach(BlockBreakEvent::cancel)
                .launchIn(mainScope)

            val committedLocations = mutableSetOf<Location>()
            val removalQueue = ArrayDeque<FulfilledPlacementCommit>()
            placementChannel.consumeAsFlow()
                .filter { placement -> committedLocations.add(placement.pos) }
                .conflate()
                .onEach { placement -> removalQueue += placement.placementRef.commitInternal(placement.pos.block) }
                .launchIn(mainScope)

            fun undoCommit(placement: FulfilledPlacementCommit) {
                placement.placementRef.undoCommitInternal(placement)
                committedLocations.remove(placement.capturedState.location)
            }

            placementChannel.invokeOnClose {
                committedLocations.clear()
                removalQueue.forEach(::undoCommit)
                removalQueue.clear()
            }

            terix.whileEnabled(terix.minecraftDispatcher) {
                val now = now()
                val itr = removalQueue.iterator()
                while (itr.hasNext()) {
                    val element = itr.next()
                    val (ref, captured, at) = element
                    if (ref.removablePredicate is Some && !ref.removablePredicate.value(captured.location)) continue
                    if (ref.expeditedPredicate is Some && ref.expeditedPredicate.value(captured.location) ||
                        at <= now ||
                        ref.changedOrigin() ||
                        ref.changedWorld(element)
                    ) {
                        undoCommit(element)
                        itr.remove()
                    }
                }
            }

            terix.eventFlow<PluginDisableEvent>()
                .filter { event -> event.plugin === terix }
                .onEach { placementChannel.close() }
                .launchIn(mainScope)
        }
    }

    public interface Immutable

    public interface BlockDataProvider {
        public val placementData: BlockData
    }

    public interface DurationLimited {
        public val placementDuration: Duration
    }
}

public fun <A> A.tempPlacement(
    removalCallback: Lambda? = null,
    commitCallback: Lambda? = null,
    removablePredicate: PosPredicate? = null,
    expeditedPredicate: PosPredicate? = null
): TemporaryPlacement where A : Ability, A : TemporaryPlacement.BlockDataProvider {
    return TemporaryPlacement(
        abilityRef = this,
        replacementData = this.placementData.clone(),
        removeAfter = (this as? TemporaryPlacement.DurationLimited)?.placementDuration ?: Duration.INFINITE,
        commitCallback = commitCallback.toOption(),
        removalCallback = removalCallback.toOption(),
        removablePredicate = removablePredicate.toOption(),
        expeditedPredicate = expeditedPredicate.toOption()
    )
}

// public class CancellableExpeditableDelayQueue : AbstractQueue<FulfilledPlacementCommit>(), BlockingQueue<FulfilledPlacementCommit> {
//    @Transient private val lock = ReentrantLock()
//    private val q = PriorityQueue<FulfilledPlacementCommit>()
//
//    private var leader: Thread? = null
//    private val available = lock.newCondition()
//
//    override fun add(element: FulfilledPlacementCommit): Boolean {
//        return offer(element)
//    }
//
//    override fun offer(e: FulfilledPlacementCommit): Boolean {
//        val lock = lock
//        lock.lock()
//        return try {
//            q.offer(e)
//            if (q.peek() === e) {
//                leader = null
//                available.signal()
//            }
//            true
//        } finally {
//            lock.unlock()
//        }
//    }
//
//    override fun put(e: FulfilledPlacementCommit) {
//        offer(e)
//    }
//
//    override fun offer(e: FulfilledPlacementCommit, timeout: Long, unit: TimeUnit): Boolean {
//        return offer(e)
//    }
//
//    override fun poll(): FulfilledPlacementCommit? {
//        val lock = lock
//        lock.lock()
//        return try {
//            val first = q.peek()
//            when {
//                first == null -> null
//                first.placementRef.expeditedPredicate(first.capturedState.location) -> q.poll()
//            }
//
//            if (first == null || first.getDelay(TimeUnit.NANOSECONDS) > 0) null else q.poll()
//        } finally {
//            lock.unlock()
//        }
//    }
//
//    @Throws(InterruptedException::class)
//    override fun take(): FulfilledPlacementCommit {
//        val lock = lock
//        lock.lockInterruptibly()
//        try {
//            while (true) {
//                var first = q.peek()
//                if (first == null) available.await() else {
//                    val delay = first.getDelay(TimeUnit.NANOSECONDS)
//                    if (delay <= 0L) return q.poll()
//                    first = null // don't retain ref while waiting
//                    if (leader != null) available.await() else {
//                        val thisThread = Thread.currentThread()
//                        leader = thisThread
//                        try {
//                            available.awaitNanos(delay)
//                        } finally {
//                            if (leader === thisThread) leader = null
//                        }
//                    }
//                }
//            }
//        } finally {
//            if (leader == null && q.peek() != null) available.signal()
//            lock.unlock()
//        }
//    }
//
//    @Throws(InterruptedException::class)
//    override fun poll(timeout: Long, unit: TimeUnit): FulfilledPlacementCommit {
//        var nanos = unit.toNanos(timeout)
//        val lock = lock
//        lock.lockInterruptibly()
//        try {
//            while (true) {
//                var first = q.peek()
//                if (first == null) {
//                    nanos = if (nanos <= 0L) return null else available.awaitNanos(nanos)
//                } else {
//                    val delay = first.getDelay(TimeUnit.NANOSECONDS)
//                    if (delay <= 0L) return q.poll()
//                    if (nanos <= 0L) return null
//                    first = null // don't retain ref while waiting
//                    if (nanos < delay || leader != null) nanos = available.awaitNanos(nanos) else {
//                        val thisThread = Thread.currentThread()
//                        leader = thisThread
//                        nanos -= try {
//                            val timeLeft = available.awaitNanos(delay)
//                            delay - timeLeft
//                        } finally {
//                            if (leader === thisThread) leader = null
//                        }
//                    }
//                }
//            }
//        } finally {
//            if (leader == null && q.peek() != null) available.signal()
//            lock.unlock()
//        }
//    }
//
//    override fun peek(): FulfilledPlacementCommit {
//        val lock = lock
//        lock.lock()
//        return try {
//            q.peek()
//        } finally {
//            lock.unlock()
//        }
//    }
//
//    override val size: Int get() {
//        val lock = lock
//        lock.lock()
//        return try {
//            q.size
//        } finally {
//            lock.unlock()
//        }
//    }
//
//    override fun drainTo(c: MutableCollection<in FulfilledPlacementCommit>): Int {
//        return drainTo(c, Int.MAX_VALUE)
//    }
//
//    override fun drainTo(c: MutableCollection<in FulfilledPlacementCommit>, maxElements: Int): Int {
//        require(c !== this) { "Cannot drain to self" }
//        if (maxElements <= 0) return 0
//        val lock = lock
//        lock.lock()
//        return try {
//            var n = 0
//            var first: FulfilledPlacementCommit = null
//            while (n < maxElements && q.peek()
//                .also { first = it } != null && first!!.getDelay(TimeUnit.NANOSECONDS) <= 0
//            ) {
//                c.add(first!!) // In this order, in case add() throws.
//                q.poll()
//                ++n
//            }
//            n
//        } finally {
//            lock.unlock()
//        }
//    }
//
//    override fun clear() {
//        val lock = lock
//        lock.lock()
//        try {
//            q.clear()
//        } finally {
//            lock.unlock()
//        }
//    }
//
//    override fun remainingCapacity(): Int {
//        return Int.MAX_VALUE
//    }
//
//    override fun toArray(): Array<Any> {
//        val lock = lock
//        lock.lock()
//        return try {
//            q.toArray()
//        } finally {
//            lock.unlock()
//        }
//    }
//
//    override fun <T> toArray(a: Array<T>): Array<T> {
//        val lock = lock
//        lock.lock()
//        return try {
//            q.toArray(a)
//        } finally {
//            lock.unlock()
//        }
//    }
//
//    public override fun remove(element: FulfilledPlacementCommit): Boolean {
//        val lock = lock
//        lock.lock()
//        return try {
//            q.remove(o)
//        } finally {
//            lock.unlock()
//        }
//    }
//
//    public fun removeEQ(o: Any) {
//        val lock = lock
//        lock.lock()
//        try {
//            val it = q.iterator()
//            while (it.hasNext()) {
//                if (o === it.next()) {
//                    it.remove()
//                    break
//                }
//            }
//        } finally {
//            lock.unlock()
//        }
//    }
//
//    override fun iterator(): MutableIterator<FulfilledPlacementCommit> {
//        return Itr(toArray())
//    }
//
//    private inner class Itr(array: Array<Any>) :
//        MutableIterator<FulfilledPlacementCommit> {
//        val array: Array<Any>
//        var cursor = // index of next element to return
//            0
//        var lastRet: Int
//
//        init {
//            lastRet = -1
//            this.array = array
//        }
//
//        override fun hasNext(): Boolean {
//            return cursor < array.size
//        }
//
//        override fun next(): FulfilledPlacementCommit {
//            if (cursor >= array.size) throw NoSuchElementException()
//            return array[cursor++.also { lastRet = it }] as E
//        }
//
//        override fun remove() {
//            check(lastRet >= 0)
//            removeEQ(array[lastRet])
//            lastRet = -1
//        }
//    }
// }
