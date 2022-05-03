package dev.racci.terix.api.origins

import com.destroystokyo.paper.MaterialSetTag
import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent
import com.destroystokyo.paper.event.entity.PhantomPreSpawnEvent
import com.destroystokyo.paper.event.entity.ProjectileCollideEvent
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent
import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent
import com.destroystokyo.paper.event.player.PlayerJumpEvent
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import com.github.benmanes.caffeine.cache.Caffeine
import dev.racci.minix.api.annotations.MinixDsl
import dev.racci.minix.api.events.PlayerDoubleOffhandEvent
import dev.racci.minix.api.events.PlayerEnterLiquidEvent
import dev.racci.minix.api.events.PlayerExitLiquidEvent
import dev.racci.minix.api.events.PlayerShiftDoubleOffhandEvent
import dev.racci.minix.api.events.PlayerShiftLeftClickEvent
import dev.racci.minix.api.events.PlayerShiftOffhandEvent
import dev.racci.minix.api.events.PlayerShiftRightClickEvent
import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.plugin.MinixPlugin
import dev.racci.minix.api.utils.collections.MultiMap
import dev.racci.minix.api.utils.collections.multiMapOf
import dev.racci.minix.api.utils.getKoin
import dev.racci.minix.api.utils.unsafeCast
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.dsl.AttributeModifierBuilder
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.dsl.TimedAttributeBuilder
import dev.racci.terix.api.dsl.TitleBuilder
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.origins.enums.KeyBinding
import dev.racci.terix.api.origins.enums.Trigger
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityResurrectEvent
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.event.entity.EntityToggleSwimEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerRiptideEvent
import org.bukkit.potion.PotionEffect
import org.spigotmc.event.entity.EntityDismountEvent
import org.spigotmc.event.entity.EntityMountEvent
import java.time.Duration
import kotlin.reflect.KClass

abstract class AbstractOrigin : WithPlugin<MinixPlugin> {
    abstract override val plugin: MinixPlugin

    private val builderCache = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(1))
        .build<KClass<*>, Any>() { kClass -> kClass.constructors.first().call(this) }
    private inline fun <reified T> builder(): T = builderCache[T::class].unsafeCast()

    // TODO: Combined all modifiers into one map
    val attributeModifiers: MultiMap<Trigger, Pair<Attribute, AttributeModifier>> by lazy(::multiMapOf)
    val titles: MutableMap<Trigger, TitleBuilder> by lazy(::mutableMapOf)
    val potions: MultiMap<Trigger, PotionEffect> by lazy(::multiMapOf)
    val damageTicks: MutableMap<Trigger, Double> by lazy(::mutableMapOf)
    val triggerBlocks: MutableMap<Trigger, suspend (Player) -> Unit> by lazy(::mutableMapOf)
    val damageMultipliers: MutableMap<EntityDamageEvent.DamageCause, Double> by lazy(::mutableMapOf)
    val damageActions: MutableMap<EntityDamageEvent.DamageCause, suspend EntityDamageEvent.() -> Unit> by lazy(::mutableMapOf)
    val foodPotions: MultiMap<Material, PotionEffect> by lazy(::multiMapOf)
    val foodAttributes: MultiMap<Material, TimedAttributeBuilder> by lazy(::multiMapOf)
    val foodMultipliers: MutableMap<Material, Int> by lazy(::mutableMapOf)
    val abilities: MultiMap<KeyBinding, AbstractAbility> by lazy(::multiMapOf)

    lateinit var itemMaterial: Material
    lateinit var itemName: Component
    lateinit var itemLore: List<Component>

    open val name: String = this::class.simpleName ?: "Unknown"
    open val colour: TextColor = NamedTextColor.WHITE
    open val displayName: Component by lazy { Component.text(name).color(colour) }

    open val hurtSound: Key = Key.key("entity.player.hurt")
    open val deathSound: Key = Key.key("entity.player.death")

    open val nightVision: Boolean = false
    open val waterBreathing: Boolean = false
    open val fireImmune: Boolean = false

    open val permission: String? = null
    open val becomeOriginTitle: TitleBuilder? = null

    /**
     * Checks if the player has permission for this origin.
     *
     * @param player The player to check.
     * @return True if the player has permission, false otherwise.
     */
    open fun hasPermission(player: Player) = permission?.let(player::hasPermission) ?: true

    open suspend fun onRegister() {}

    /**
     * Called when the player respawns
     */
    open suspend fun onRespawn(event: PlayerRespawnEvent) {}

    /**
     * Called when the player changes origins
     * Any special effects or caches should be cleared here.
     */
    open suspend fun onChange(event: PlayerOriginChangeEvent) {}

    /**
     * Called after the player has respawned
     */
    open suspend fun onPostRespawn(event: PlayerPostRespawnEvent) {}

    /**
     * Called on the players death
     */
    open suspend fun onDeath(event: PlayerDeathEvent) {}

    /**
     * Called when the player enters a liquid block
     */
    open suspend fun onEnterLiquid(event: PlayerEnterLiquidEvent) {}

    /**
     * Called when the player exits a liquid block
     */
    open suspend fun onExitLiquid(event: PlayerExitLiquidEvent) {}

    /**
     * Called when the players breaks a block
     */
    open suspend fun onBreakBlock(event: BlockBreakEvent) {}

    /**
     * Call when the player places a block
     */
    open suspend fun onPlaceBlock(event: BlockPlaceEvent) {}

    /**
     * Called when the player takes damage.
     */
    open suspend fun onDamage(event: EntityDamageEvent) {}

    /**
     * Called when the player damages another entity
     */
    open suspend fun onDamageEntity(event: EntityDamageByEntityEvent) {}

    /**
     * Called when the player is damaged by an entity
     */
    open suspend fun onDamageByEntity(event: EntityDamageByEntityEvent) {}

    /**
     * Called when the player kills an entity
     */
    open suspend fun onKillEntity(event: EntityDeathEvent) {}

    /**
     * Called when the players launches a projectile
     */
    open suspend fun onProjectileLaunch(event: PlayerLaunchProjectileEvent) {}

    /**
     * Called when a players projectile lands
     */
    open suspend fun onProjectileLand(event: ProjectileHitEvent) {}

    open suspend fun onProjectileCollide(event: ProjectileCollideEvent) {}

    open suspend fun onArmourChange(event: PlayerArmorChangeEvent) {}

    open suspend fun onChangeWorld(event: PlayerChangedWorldEvent) {}

    open suspend fun onFish(event: PlayerFishEvent) {}

    open suspend fun onItemDamage(event: PlayerItemDamageEvent) {}

    open suspend fun onRiptide(event: PlayerRiptideEvent) {}

    open suspend fun onCombust(event: EntityCombustEvent) {}

    open suspend fun onResurrect(event: EntityResurrectEvent) {}

    open suspend fun onToggleSwim(event: EntityToggleSwimEvent) {}

    open suspend fun onToggleGlide(event: EntityToggleGlideEvent) {}

    open suspend fun onJump(event: PlayerJumpEvent) {}

    open suspend fun onKnockback(event: EntityKnockbackByEntityEvent) {}

    open suspend fun onPhantomSpawn(event: PhantomPreSpawnEvent) {}

    open suspend fun onElytraBoost(event: PlayerElytraBoostEvent) {}

    open suspend fun onEntityMount(event: EntityMountEvent) {}

    open suspend fun onEntityDismount(event: EntityDismountEvent) {}

    open suspend fun onDoubleOffhand(event: PlayerDoubleOffhandEvent) {}

    open suspend fun onShiftDoubleOffhand(event: PlayerShiftDoubleOffhandEvent) {}

    open suspend fun onShiftOffhand(event: PlayerShiftOffhandEvent) {}

    open suspend fun onShiftLeftClick(event: PlayerShiftLeftClickEvent) {}

    open suspend fun onShiftRightClick(event: PlayerShiftRightClickEvent) {}

    @MinixDsl
    protected suspend fun potions(builder: suspend AbstractOrigin.PotionsBuilder.() -> Unit) {
        builder(builder())
    }

    @MinixDsl
    protected suspend fun attributes(builder: suspend AbstractOrigin.AttributeBuilder.() -> Unit) {
        builder(builder())
    }

    @MinixDsl
    protected suspend fun title(builder: suspend AbstractOrigin.TimeTitleBuilder.() -> Unit) {
        builder(builder())
    }

    @MinixDsl
    protected suspend fun damage(builder: suspend AbstractOrigin.DamageBuilder.() -> Unit) {
        builder(builder())
    }

    @MinixDsl
    protected suspend fun food(builder: suspend AbstractOrigin.FoodBuilder.() -> Unit) {
        builder(builder())
    }

    @MinixDsl
    protected suspend fun item(builder: suspend AbstractOrigin.ItemBuilder.() -> Unit) {
        builder(builder())
    }

    protected inner class PotionsBuilder {

        /**
         * Adds a potion effect to this trigger.
         * ## Note: The key of the potion effect will always be overridden.
         */
        @MinixDsl
        infix fun Trigger.causes(builder: PotionEffectBuilder.() -> Unit) {
            val potionEffectBuilder = PotionEffectBuilder()
            potionEffectBuilder.builder()
            potionEffectBuilder.originKey(this@AbstractOrigin, this)
            potions.put(this, potionEffectBuilder.build())
        }
    }

    protected inner class AttributeBuilder {

        /* Sets the base values of this origins attributes. */
        @MinixDsl
        infix fun Attribute.setBase(builder: AttributeModifierBuilder.() -> Unit) {
            val modifierBuilder = AttributeModifierBuilder()
            modifierBuilder.builder()
            modifierBuilder.attribute = this
            modifierBuilder.name = "origin_modifier_${this@AbstractOrigin.name.lowercase()}_${Trigger.ON.name.lowercase()}" // Use Enum incase of changes of name or something.
            attributeModifiers.put(Trigger.ON, this to modifierBuilder.build())
        }

        /* Adds an attributeModifier to this trigger. */
        @MinixDsl
        infix fun Trigger.causes(builder: AttributeModifierBuilder.() -> Unit) {
            val attributeModifierBuilder = AttributeModifierBuilder()
            attributeModifierBuilder.builder()
            attributeModifierBuilder.name = "origin_modifier_${this@AbstractOrigin.name.lowercase()}_${this@causes.name.lowercase()}"
            attributeModifiers.put(this, attributeModifierBuilder.attribute to attributeModifierBuilder.build())
        }
    }

    protected inner class TimeTitleBuilder {

        /**
         * Triggers a title when this trigger is activated.
         */
        @MinixDsl
        infix fun Trigger.causes(builder: TitleBuilder.() -> Unit) {
            val titleBuilder = TitleBuilder()
            titleBuilder.builder()
            titles[this] = titleBuilder
        }
    }

    protected inner class DamageBuilder {

        /**
         * When the player is damage and one of these triggers is satisfied the
         * event will be passed to the block for modification.
         */
        @MinixDsl
        infix fun Trigger.invokes(builder: suspend (Player) -> Unit) { triggerBlocks[this] = builder }

        /**
         * Damages the player by this amount when this trigger happens.
         * Most useful for triggers such as water, sunlight and darkness which
         * are called once per second.
         */
        @MinixDsl
        infix fun Trigger.ticks(damage: Double) { damageTicks[this] = damage }

        /**
         * Multiply the damage dealt to the player with this multiplier.
         * Setting this to 0 will cancel the event.
         */
        @MinixDsl
        infix fun EntityDamageEvent.DamageCause.multiplied(multiplier: Double) { damageMultipliers[this] = multiplier }

        /**
         * The same as [multiplied] but sets it for all items within the collection.
         */
        @MinixDsl
        infix fun Collection<EntityDamageEvent.DamageCause>.multiplied(multiplier: Double) { this.forEach { it.multiplied(multiplier) } }

        @MinixDsl
        infix fun EntityDamageEvent.DamageCause.triggers(action: suspend EntityDamageEvent.() -> Unit) { damageActions[this] = action }
    }

    protected inner class FoodBuilder {

        @MinixDsl
        infix fun MaterialSetTag.effects(builder: PotionEffectBuilder.() -> Unit) { values.forEach { it.effects(builder) } }

        @MinixDsl
        infix fun Material.effects(builder: PotionEffectBuilder.() -> Unit) { foodPotions.put(this, PotionEffectBuilder().apply { builder(); originKey(this@AbstractOrigin.name, "name") }.build()) }

        @MinixDsl
        infix fun MaterialSetTag.applies(builder: TimedAttributeBuilder.() -> Unit) { values.forEach { it.applies(builder) } }

        @MinixDsl
        infix fun Material.applies(builder: TimedAttributeBuilder.() -> Unit) { foodAttributes.put(this, TimedAttributeBuilder().apply(builder)) }

        @MinixDsl
        infix fun MaterialSetTag.multiplied(multiplier: Int) { values.forEach { it.multiplied(multiplier) } }

        @MinixDsl
        infix fun Material.multiplied(value: Int) { foodMultipliers[this] = value }
    }

    protected inner class ItemBuilder {

        infix fun AbstractOrigin.named(component: Component) { itemName = component }

        infix fun AbstractOrigin.material(material: Material) { itemMaterial = material }

        infix fun AbstractOrigin.lore(builder: MutableMap<Int, Component>.() -> Unit) {
            itemLore = mutableMapOf<Int, Component>().apply(builder).values.toList()
        }
    }

    protected inner class AbilityBuilder {

        inline fun <reified T : AbstractAbility> KeyBinding.add() = getKoin().get<OriginService>()
    }

    override fun toString(): String {
        return "Origin(name='$name', itemName=$itemName, itemMaterial=$itemMaterial, itemLore=$itemLore, " +
            "abilities=$abilities, triggerBlocks=$triggerBlocks, damageTicks=$damageTicks, damageMultipliers=$damageMultipliers, " +
            "damageActions=$damageActions, foodPotions=$foodPotions, foodAttributes=$foodAttributes, foodMultipliers=$foodMultipliers)"
    }
}
