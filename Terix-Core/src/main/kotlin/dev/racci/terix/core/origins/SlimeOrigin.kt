package dev.racci.terix.core.origins

import com.github.benmanes.caffeine.cache.Caffeine
import dev.racci.minix.api.annotations.RunAsync
import dev.racci.minix.api.collections.PlayerMap
import dev.racci.minix.api.data.enums.LiquidType
import dev.racci.minix.api.events.player.PlayerLiquidEnterEvent
import dev.racci.minix.api.events.player.PlayerLiquidExitEvent
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.dropItem
import dev.racci.minix.api.extensions.isSword
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.minix.api.extensions.taskAsync
import dev.racci.minix.api.scheduler.CoroutineTask
import dev.racci.minix.api.utils.now
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.Terix
import dev.racci.terix.api.annotations.OriginEventSelector
import dev.racci.terix.api.dsl.AttributeModifierBuilder
import dev.racci.terix.api.dsl.dslMutator
import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.sounds.SoundEffect
import dev.racci.terix.api.origins.states.State
import dev.racci.terix.core.extensions.inWater
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType
import java.time.Duration
import kotlin.reflect.KFunction
import kotlin.time.Duration.Companion.seconds

// TODO -> Top food is a bottle of water.
// TODO -> Water melon, Honey, and soup / stew (No rabbit stew) is good too.
// TODO -> CAke.
public class SlimeOrigin(override val plugin: Terix) : Origin() {

    override val name: String = "Slime"
    override val colour: TextColor = TextColor.fromHexString("#61f45a")!!

    private val healthCache = HealthCache()
    private val damageCache = PlayerMap<Instant>()

    override suspend fun handleRegister() {
        sounds.hurtSound = SoundEffect("entity.slime.hurt")
        sounds.deathSound = SoundEffect("entity.slime.death")
        sounds.ambientSound = SoundEffect("entity.slime.ambient")

        potions {
            State.CONSTANT += dslMutator {
                type = PotionEffectType.JUMP
                amplifier = 4
                duration = kotlin.time.Duration.INFINITE
                ambient = true
            }
        }
        attributes {
            Attribute.GENERIC_MAX_HEALTH *= 0.8
        }
        damage {
            EntityDamageEvent.DamageCause.FALL += { cause ->
                if (cause.entity.castOrThrow<Player>().toNMS().random.nextBoolean()) {
                    cause.cancel()
                }
            }
            listOf(
                EntityDamageEvent.DamageCause.LAVA,
                EntityDamageEvent.DamageCause.FIRE,
                EntityDamageEvent.DamageCause.FIRE_TICK,
                EntityDamageEvent.DamageCause.HOT_FLOOR
            ) *= 1.5
        }
        item {
            material = Material.SLIME_BALL
            lore = "<green>Slime lol"
        }
    }

    @RunAsync
    @OriginEventSelector(EventSelector.ENTITY)
    public fun EntityDamageEvent.handle() {
        damageCache[entity.castOrThrow()] = now()
    }

    @OriginEventSelector(EventSelector.ENTITY)
    public fun EntityDamageByEntityEvent.handle() {
        with(this.damager as? LivingEntity ?: return) {
            if (this.equipment == null) return
            if (this.equipment!!.itemInMainHand.type.isSword) {
                this@handle.damage *= 1.5
            }
        }
    }

    @OriginEventSelector(EventSelector.PLAYER)
    public fun PlayerLiquidEnterEvent.handle() {
        if (newType != LiquidType.WATER) return

        healthCache.putEnterTask(player) {
            do {
                task(player, HealthCache::inc) {
                    if (damageCache[player] == null || damageCache[player]!!.plus(DAMAGE_WAIT_TIME) > now()) {
                        player.health += 0.5
                    }
                }
            } while (healthCache[player].amount != MAX_HEALTH_BONUS && player.inWater)
        }
    }

    @OriginEventSelector(EventSelector.PLAYER)
    public fun PlayerLiquidExitEvent.onExitLiquid() {
        if (previousType != LiquidType.WATER) return

        healthCache.putExitTask(player) {
            do {
                task(player, HealthCache::dec)
            } while (healthCache[player].amount > 0.0 && !player.inWater)
        }
    }

    private suspend fun task(
        player: Player,
        function: KFunction<Unit>,
        extraAction: () -> Unit = {}
    ) {
        val instance = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!

        instance.removeModifier(healthCache[player])
        function.call(healthCache, player)
        instance.addModifier(healthCache[player])

        extraAction()

        delay(1.seconds)
    }

    private val lastDamage = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(15))
        .build<Player, Instant>()

    @OriginEventSelector(EventSelector.ENTITY)
    public fun onDamage(event: EntityDamageEvent) {
        val last = lastDamage.getIfPresent(event.entity.castOrThrow())
        val now = now()

        if (last != null && (last + 15.seconds) > now()) return

        lastDamage.put(event.entity.castOrThrow(), now)
        event.entity.location.dropItem(ItemStack(Material.SLIME_BALL))
    }

    private inner class HealthCache {
        private val modifierCache = Array(MAX_HEALTH_BONUS.toInt()) { i ->
            dslMutator<AttributeModifierBuilder> {
                amount = convertIndex(i)
                operation = AttributeModifier.Operation.ADD_NUMBER
            }.asNew().originName("slime", "waterbonus").get()
        }

        private val playerMap: PlayerMap<Int> = PlayerMap()
        private val runningTask: PlayerMap<Pair<CoroutineTask, Boolean>> = PlayerMap()

        operator fun get(player: Player): AttributeModifier = this.modifierCache[this.playerMap.computeIfAbsent(player) { 0 }]

        fun inc(player: Player) {
            playerMap[player] = convertAmount(this[player].amount) + 1
        }

        fun dec(player: Player) {
            playerMap[player] = convertAmount(this[player].amount) - 1
        }

        fun putEnterTask(
            player: Player,
            block: suspend () -> Unit
        ) {
            if (runningTask[player]?.second == true) return
            runningTask.computeIfAbsent(player) { newTask(player, block) to true }
        }

        fun putExitTask(
            player: Player,
            block: suspend () -> Unit
        ) {
            if (runningTask[player]?.second == false) return
            runningTask.compute(player) { _, task ->
                task?.first?.cancel()
                newTask(player, block) to false
            }
        }

        private fun newTask(
            player: Player,
            block: suspend () -> Unit
        ) = taskAsync(1.seconds) {
            block()
            runningTask.remove(player)
        }

        private fun convertIndex(index: Int): Double = index / 2.0

        private fun convertAmount(amount: Double): Int = (amount * 2).toInt()
    }

    private companion object {
        const val MAX_HEALTH_BONUS = 5.0
        val DAMAGE_WAIT_TIME = 8.seconds
    }
}
