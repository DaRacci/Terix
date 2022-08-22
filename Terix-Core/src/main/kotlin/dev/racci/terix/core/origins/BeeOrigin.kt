package dev.racci.terix.core.origins

import dev.racci.minix.api.collections.PlayerMap
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.utils.now
import dev.racci.minix.api.utils.unsafeCast
import dev.racci.terix.api.Terix
import dev.racci.terix.api.dsl.TimedAttributeBuilder
import dev.racci.terix.api.dsl.TitleBuilder
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.sounds.SoundEffect
import dev.racci.terix.core.data.Lang
import dev.racci.terix.core.data.Lang.PartialComponent.Companion.message
import kotlinx.datetime.Instant
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.entity.EntityPotionEffectEvent.Cause
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.koin.core.component.get
import kotlin.time.Duration.Companion.seconds

// TODO -> All normal food gives half a heart of damage, they can only eat flowers.
// TODO -> Some flowers are edible, some flowers are potion effects.
class BeeOrigin(override val plugin: Terix) : Origin() {
    private val stingerInstant = PlayerMap<Instant>()
    private val lowerFood = mutableSetOf<Player>()

    override val name = "Bee"
    override val colour = TextColor.fromHexString("#fc9f2f")!!

    override suspend fun onRegister() {
        sounds.hurtSound = SoundEffect("entity.bee.hurt")
        sounds.deathSound = SoundEffect("entity.bee.death")
        sounds.ambientSound = SoundEffect("entity.bee.ambient")

        item {
            material = Material.HONEYCOMB
            lore = """
                <gold>A bee is a type of bee.
                <gold>It is a type of bee.
            """.trimIndent()
        }
    }

    override suspend fun onDamageEntity(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as Player
        val victim = event.entity as? LivingEntity ?: return
        stingerAttack(attacker, victim)
    }

    override suspend fun onPotionEffect(event: EntityPotionEffectEvent) {
        if (event.action != EntityPotionEffectEvent.Action.ADDED) return
        if (event.cause in BANNED_POTION_CAUSES) event.cancel()
    }

    override suspend fun onFoodChange(event: FoodLevelChangeEvent) {
        if (lowerFood.remove(event.entity.unsafeCast())) event.cancel()
    }

    override suspend fun onItemConsume(event: PlayerItemConsumeEvent) {
        if (antiPotion(event)) return
        lowerFood.add(event.player)
    }

    private fun stingerAttack(
        attacker: Player,
        victim: LivingEntity
    ) {
        if (attacker.inventory.itemInMainHand.type != Material.AIR) return

        val now = now()
        val current = stingerInstant.compute(attacker) { _, value ->
            if (value != null && value + STINGER_COOLDOWN >= now) value else now
        }

        if (now === current) return

        attacker.damage(0.5)
        victim.damage(0.5)
        victim.addPotionEffect(PotionEffect(PotionEffectType.POISON, 60, 0))
    }

    private fun antiPotion(event: PlayerItemConsumeEvent): Boolean {
        val potion = event.item
        if (potion.itemMeta is PotionMeta) {
            event.cancel()
            get<Lang>().origin.bee["potion"] message event.player
            return true
        }

        return false
    }

    // TODO: Possible groups
    @Suppress("kotlin:S1151")
    private val Material.isFlower
        get() = when (this) {
            Material.DANDELION,
            Material.POPPY,
            Material.BLUE_ORCHID,
            Material.ALLIUM,
            Material.AZURE_BLUET,
            Material.RED_TULIP,
            Material.ORANGE_TULIP,
            Material.WHITE_TULIP,
            Material.PINK_TULIP,
            Material.OXEYE_DAISY,
            Material.CORNFLOWER,
            Material.LILY_OF_THE_VALLEY,
            Material.WITHER_ROSE,
            Material.LILAC,
            Material.ROSE_BUSH,
            Material.PEONY -> true
            else -> false
        }

    private fun Material.getEffect(): FlowerEffect? {
        if (!this.isFlower) return null

        return when (this) {
            Material.DANDELION -> TODO("Dandelion effect")
            Material.POPPY -> TODO("Poppy effect")
            Material.BLUE_ORCHID -> TODO("Blue Orchid effect")
            Material.ALLIUM -> TODO("Allium effect")
            Material.AZURE_BLUET -> TODO("Azure Bluet effect")
            Material.RED_TULIP -> TODO("Red Tulip effect")
            Material.ORANGE_TULIP -> TODO("Orange Tulip effect")
            Material.WHITE_TULIP -> TODO("White Tulip effect")
            Material.PINK_TULIP -> TODO("Pink Tulip effect")
            Material.OXEYE_DAISY -> TODO("Oxeye Daisy effect")
            Material.CORNFLOWER -> TODO("Cornflower effect")
            Material.LILY_OF_THE_VALLEY -> TODO("Lily Of The Valley effect")
            Material.WITHER_ROSE -> TODO("Wither Rose effect")
            Material.LILAC -> TODO("Lilac effect")
            Material.ROSE_BUSH -> TODO("Rose Bush effect")
            Material.PEONY -> TODO("Peony effect")
            else -> null
        }
    }

    companion object {
        val STINGER_COOLDOWN = 10.seconds
        val BANNED_POTION_CAUSES = arrayOf(
            Cause.AREA_EFFECT_CLOUD,
            Cause.BEACON,
            Cause.CONDUIT,
            Cause.POTION_DRINK,
            Cause.POTION_SPLASH
        )
    }

    private class FlowerEffect {
        var potionEffect: PotionEffect? = null
        var attribute: TimedAttributeBuilder? = null
        var title: TitleBuilder? = null

        fun apply(player: Player) {
            attribute?.invoke(player)
            potionEffect?.apply(player)
            title?.invoke(player)
        }
    }
}
