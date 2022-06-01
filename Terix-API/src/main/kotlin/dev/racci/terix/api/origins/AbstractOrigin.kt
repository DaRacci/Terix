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
import dev.racci.minix.api.events.PlayerDoubleLeftClickEvent
import dev.racci.minix.api.events.PlayerDoubleOffhandEvent
import dev.racci.minix.api.events.PlayerDoubleRightClickEvent
import dev.racci.minix.api.events.PlayerEnterLiquidEvent
import dev.racci.minix.api.events.PlayerExitLiquidEvent
import dev.racci.minix.api.events.PlayerLeftClickEvent
import dev.racci.minix.api.events.PlayerOffhandEvent
import dev.racci.minix.api.events.PlayerRightClickEvent
import dev.racci.minix.api.events.PlayerShiftDoubleLeftClickEvent
import dev.racci.minix.api.events.PlayerShiftDoubleOffhandEvent
import dev.racci.minix.api.events.PlayerShiftDoubleRightClickEvent
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
import dev.racci.terix.api.Terix
import dev.racci.terix.api.dsl.AttributeModifierBuilder
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.dsl.TimedAttributeBuilder
import dev.racci.terix.api.dsl.TitleBuilder
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.origins.enums.KeyBinding
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.api.origins.sounds.SoundEffects
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.block.Biome
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
import org.bukkit.event.inventory.InventoryOpenEvent
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

    open val name: String = this::class.simpleName?.withIndex()?.takeWhile {
        it.value.isLetter() || it.index == 0
    }?.map(IndexedValue<Char>::value)?.toString() ?: "Unknown"
    open val colour: TextColor = NamedTextColor.WHITE
    open val displayName: Component by lazy { Component.text(name).color(colour) }

    open val nightVision: Boolean = false
    open val waterBreathing: Boolean = false
    open val fireImmune: Boolean = false

    open val permission: String? = null
    open val becomeOriginTitle: TitleBuilder? = null

    // TODO: Combined all modifiers into one map or something like that
    val attributeModifiers: MultiMap<Trigger, Pair<Attribute, AttributeModifier>> by lazy(::multiMapOf)
    val titles: MutableMap<Trigger, TitleBuilder> by lazy(::mutableMapOf)
    val potions: MultiMap<Trigger, PotionEffect> by lazy(::multiMapOf)
    val damageTicks: MutableMap<Trigger, Double> by lazy(::mutableMapOf)
    val triggerBlocks: MutableMap<Trigger, suspend (Player) -> Unit> by lazy(::mutableMapOf)

    val damageActions: MutableMap<EntityDamageEvent.DamageCause, suspend EntityDamageEvent.() -> Unit> by lazy(::mutableMapOf)

    val foodBlocks: MutableMap<Material, suspend (Player) -> Unit> by lazy(::mutableMapOf)
    val foodPotions: MultiMap<Material, PotionEffect> by lazy(::multiMapOf)
    val foodAttributes: MultiMap<Material, TimedAttributeBuilder> by lazy(::multiMapOf)
    val foodMultipliers: MutableMap<Material, Double> by lazy(::mutableMapOf)

    val abilities: MutableMap<KeyBinding, AbstractAbility> by lazy(::mutableMapOf)

    val item: OriginItem = OriginItem()
    val sounds: SoundEffects = SoundEffects()

    /**
     * Checks if the player has permission for this origin.
     *
     * @param player The player to check.
     * @return True if the player has permission, false otherwise.
     */
    open fun hasPermission(player: Player) = permission?.let(player::hasPermission) ?: true

    /** Called when the origin is first registered with Terix. */
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

    /** Called when the players projectile collides with another entity. */
    open suspend fun onProjectileCollide(event: ProjectileCollideEvent) {}

    /** Called when the player changes their equipped armour. */
    open suspend fun onArmourChange(event: PlayerArmorChangeEvent) {}

    /** Called when the player changes world. */
    open suspend fun onChangeWorld(event: PlayerChangedWorldEvent) {}

    /** Called when the player attempts to catch a fish. */
    open suspend fun onFish(event: PlayerFishEvent) {}

    /** Called when the player's item obtains an extra point of damage / durability. */
    open suspend fun onItemDamage(event: PlayerItemDamageEvent) {}

    /** Called when the player uses riptide on a trident. */
    open suspend fun onRiptide(event: PlayerRiptideEvent) {}

    /** Called when the player combusts into flames. */
    open suspend fun onCombust(event: EntityCombustEvent) {}

    /** Called when the player is revived by a totem of undying. */
    open suspend fun onResurrect(event: EntityResurrectEvent) {}

    /** Called when the player toggles the swimming mode inside water. */
    open suspend fun onToggleSwim(event: EntityToggleSwimEvent) {}

    /** Called when the player toggles gliding on an elytra. */
    open suspend fun onToggleGlide(event: EntityToggleGlideEvent) {}

    /** Called when the player jumps. */
    open suspend fun onJump(event: PlayerJumpEvent) {}

    /** Called when the player receives knockback from an entity. */
    open suspend fun onKnockback(event: EntityKnockbackByEntityEvent) {}

    /** Called when a phantom spawns due to a players insomnia. */
    open suspend fun onPhantomSpawn(event: PhantomPreSpawnEvent) {}

    /** Called when the player uses fireworks to boost their speed. */
    open suspend fun onElytraBoost(event: PlayerElytraBoostEvent) {}

    /** Called when the player mounts a vehicle. */
    open suspend fun onEntityMount(event: EntityMountEvent) {}

    /** Called when the player dismounts a vehicle. */
    open suspend fun onEntityDismount(event: EntityDismountEvent) {}

    /** Called when the player left clicks. */
    open suspend fun onLeftClick(event: PlayerLeftClickEvent) {}

    /** Called when the player right clicks. */
    open suspend fun onRightClick(event: PlayerRightClickEvent) {}

    /** Called when the player uses the offhand bind. */
    open suspend fun onOffhand(event: PlayerOffhandEvent) {}

    /** Called when the player left clicks twice in quick succession. */
    open suspend fun onDoubleLeftClick(event: PlayerDoubleLeftClickEvent) {}

    /** Called when the player right clicks twice in quick succession. */
    open suspend fun onDoubleRightClick(event: PlayerDoubleRightClickEvent) {}

    /** Called when the player uses their offhand bind twice in quick succession. */
    open suspend fun onDoubleOffhand(event: PlayerDoubleOffhandEvent) {}

    /** Called when the player uses sneaks and left clicks. */
    open suspend fun onSneakLeftClick(event: PlayerShiftLeftClickEvent) {}

    /** Called when the player uses sneaks and right clicks. */
    open suspend fun onSneakRightClick(event: PlayerShiftRightClickEvent) {}

    /** Called when the player uses sneaks and uses their offhand bind. */
    open suspend fun onSneakOffhand(event: PlayerShiftOffhandEvent) {}

    /** Called when the player uses sneaks and left clicks twice in quick succession. */
    open suspend fun onSneakDoubleLeftClick(event: PlayerShiftDoubleLeftClickEvent) {}

    /** Called when the player uses sneaks and right clicks twice in quick succession. */
    open suspend fun onSneakDoubleRightClick(event: PlayerShiftDoubleRightClickEvent) {}

    /** Called when the player uses sneaks and uses their offhand bind twice in quick succession. */
    open suspend fun onSneakDoubleOffhand(event: PlayerShiftDoubleOffhandEvent) {}

    /** Called when the player opens an inventory. */
    open suspend fun onInventoryOpen(event: InventoryOpenEvent) {}

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
    protected suspend fun item(builder: suspend OriginItem.() -> Unit) {
        builder(item)
    }

    @MinixDsl
    protected suspend fun abilities(builder: suspend AbstractOrigin.AbilityBuilder.() -> Unit) {
        builder(builder())
    }

    protected inner class PotionsBuilder {

        /**
         * Adds a potion to the player while this trigger is active.
         * ### Note: The potion key will always be overwritten.
         */
        operator fun Trigger.plusAssign(builder: PotionEffectBuilder.() -> Unit) {
            val pot = PotionEffectBuilder(builder).originKey(this@AbstractOrigin, this)
            potions.put(this, pot.build())
        }
    }

    protected inner class AttributeBuilder {

        /**
         * Removes this number from the players base attributes.
         *
         * @param value The amount to remove.
         * @receiver The attribute to remove from.
         */
        operator fun Attribute.minusAssign(value: Number) = addAttribute(this, AttributeModifier.Operation.ADD_NUMBER, value, Trigger.ON)

        /**
         * Adds this number to the players base attributes.
         *
         * @param value The amount to add.
         * @receiver The attribute to add to.
         */
        operator fun Attribute.plusAssign(value: Number) = addAttribute(this, AttributeModifier.Operation.ADD_NUMBER, value, Trigger.ON)

        /**
         * Multiplies the players base attribute by this number.
         *
         * @param value The amount to multiply by.
         * @receiver The attribute to multiply.
         */
        operator fun Attribute.timesAssign(value: Number) = addAttribute(this, AttributeModifier.Operation.MULTIPLY_SCALAR_1, value.toDouble() - 1, Trigger.ON)

        /**
         * Divides the players base attribute by this number.
         *
         * @param value The amount to divide by.
         * @receiver The attribute to divide.
         */
        operator fun Attribute.divAssign(value: Number) = addAttribute(this, AttributeModifier.Operation.MULTIPLY_SCALAR_1, (1.0 / value.toDouble()) - 1, Trigger.ON)

        /**
         * Removes this number from the players attribute when this trigger is active.
         *
         * @param value The amount to remove.
         * @receiver The Trigger and Attribute to remove from.
         */
        operator fun Pair<Trigger, Attribute>.minusAssign(value: Number) = addAttribute(this.second, AttributeModifier.Operation.ADD_NUMBER, value, this.first)

        /**
         * Adds this number to the players attribute when this trigger is active.
         *
         * @param value The amount to add.
         * @receiver The Trigger and Attribute to add to.
         */
        operator fun Pair<Trigger, Attribute>.plusAssign(value: Number) = addAttribute(this.second, AttributeModifier.Operation.ADD_NUMBER, value, this.first)

        /**
         * Multiplies the players attribute by this number when this trigger is active.
         *
         * @param value The amount to multiply by.
         * @receiver The Trigger and Attribute to multiply.
         */
        operator fun Pair<Trigger, Attribute>.timesAssign(value: Number) = addAttribute(this.second, AttributeModifier.Operation.MULTIPLY_SCALAR_1, value, this.first)

        /**
         * Divides the players attribute by this number when this trigger is active.
         *
         * @param value The amount to divide by.
         * @receiver The Trigger and Attribute to divide.
         */
        operator fun Pair<Trigger, Attribute>.divAssign(value: Number) = addAttribute(this.second, AttributeModifier.Operation.MULTIPLY_SCALAR_1, 1.0 / value.toDouble(), this.first)

        private fun addAttribute(
            attribute: Attribute,
            operation: AttributeModifier.Operation,
            amount: Number,
            trigger: Trigger
        ) {
            attributeModifiers.put(
                trigger,
                attribute to AttributeModifierBuilder {
                    originName(this@AbstractOrigin, trigger)
                    this.attribute = attribute
                    this.operation = operation
                    this.amount = amount
                }.build()
            )
        }
    }

    protected inner class TimeTitleBuilder {

        /**
         * Displays this title to the player when then given trigger is activated.
         *
         * @param builder The title builder to use.
         * @receiver The trigger to activate the title.
         */
        operator fun Trigger.plusAssign(builder: TitleBuilder.() -> Unit) {
            val title = TitleBuilder()
            builder(title)
            titles[this] = title
        }

        // TODO: Title on deactivation of trigger.
    }

    protected inner class DamageBuilder {

        /**
         * Triggers this lambda when the player takes damage and this Trigger is active.
         *
         * @param builder The damage builder to use.
         * @receiver The trigger to activate the damage.
         */
        operator fun Trigger.plusAssign(builder: suspend (Player) -> Unit) { triggerBlocks[this] = builder }

        /**
         * Deals the number of damage to the player when the given trigger is activated.
         *
         * @param number The amount of damage to deal.
         * @receiver The trigger to activate the damage.
         */
        operator fun Trigger.plusAssign(number: Number) { damageTicks[this] = number.toDouble() }

        /**
         * Adds this amount of damage to the player when the player's damage cause is this.
         *
         * @param number The amount of damage to add.
         * @receiver The damage cause that is affected.
         */
        operator fun EntityDamageEvent.DamageCause.plusAssign(number: Number) { damageActions[this] = { this.damage += number.toDouble() } }

        /**
         * Minuses this amount of damage to the player when the player's damage cause is this.
         *
         * @param number The amount of damage to minus.
         * @receiver The damage cause that is affected.
         */
        operator fun EntityDamageEvent.DamageCause.minusAssign(number: Number) { damageActions[this] = { this.damage -= number.toDouble() } }

        /**
         * Multiplies this amount of damage to the player when the player's damage cause is this.
         *
         * @param number The amount of damage to multiply.
         * @receiver The damage cause that is affected.
         */
        operator fun EntityDamageEvent.DamageCause.timesAssign(number: Number) { damageActions[this] = { this.damage *= number.toDouble() } }

        /**
         * Divides this amount of damage to the player when the player's damage cause is this.
         *
         * @param number The amount of damage to divide.
         * @receiver The damage cause that is affected.
         */
        operator fun EntityDamageEvent.DamageCause.divAssign(number: Number) { damageActions[this] = { this.damage /= number.toDouble() } }

        /**
         * Runs this lambda async when the player takes damage from this causes.
         *
         * @param block The lambda to run.
         * @receiver The damage cause that is affected.
         */
        operator fun EntityDamageEvent.DamageCause.plusAssign(block: suspend (EntityDamageEvent) -> Unit) { damageActions[this] = block }

        /**
         * Adds all elements for [Trigger.plusAssign]
         *
         * @param builder The damage builder to use.
         * @receiver The triggers that activate the damage.
         */
        @JvmName("plusAssignTrigger")
        operator fun Collection<Trigger>.plusAssign(builder: suspend (Player) -> Unit) = forEach { it += builder }

        /**
         * Adds all elements to [Trigger.plusAssign]
         *
         * @param number The amount of damage to deal.
         * @receiver The triggers that activate the damage.
         */
        operator fun Collection<Trigger>.plusAssign(number: Number) = forEach { it += number }

        /**
         * Adds all elements to [EntityDamageEvent.DamageCause.plusAssign]
         */
        operator fun Collection<EntityDamageEvent.DamageCause>.plusAssign(block: suspend (EntityDamageEvent) -> Unit) = forEach { it += block }

        /**
         * Adds all elements to [EntityDamageEvent.DamageCause.plusAssign]
         * * @param number The amount of damage to deal.
         * @receiver The causes that are affected.
         */
        @JvmName("plusAssignCause")
        operator fun Collection<EntityDamageEvent.DamageCause>.plusAssign(number: Number) = forEach { it += number }

        /**
         * Adds all elements to [EntityDamageEvent.DamageCause.minusAssign]
         */
        operator fun Collection<EntityDamageEvent.DamageCause>.minusAssign(number: Number) = forEach { it -= number }

        /**
         * Adds all elements to [EntityDamageEvent.DamageCause.timesAssign]
         *
         * @param number The amount of damage to multiply.
         * @receiver The triggers that activate the damage.
         */
        operator fun Collection<EntityDamageEvent.DamageCause>.timesAssign(number: Number) = forEach { it *= number }

        /**
         * Adds all elements to [EntityDamageEvent.DamageCause.divAssign]
         *
         * @param number The amount of damage to divide.
         * @receiver The triggers that activate the damage.
         */
        operator fun Collection<EntityDamageEvent.DamageCause>.divAssign(number: Number) = forEach { it /= number }
    }

    /** A Utility class for creating food related triggers. */
    protected inner class FoodBuilder {

        /**
         * When the player consumes an item in this collection, the food level will be multiplied by this value.
         *
         * @param value The value to multiply the food level by.
         * @receiver The collection of foods.
         */
        operator fun MaterialSetTag.timesAssign(value: Number) = values.forEach { it *= value }

        /**
         * When the player consumes an item in this collection, the food level will be divided by this value.
         *
         * @param value The value to divide the food level by.
         * @receiver The collection of foods.
         */
        operator fun MaterialSetTag.divAssign(value: Number) = values.forEach { it /= value }

        /**
         * When the player consumes an item in this collection, this lambda will be run async.
         *
         * @param builder The lambda to run.
         * @receiver The collection of foods.
         */
        operator fun MaterialSetTag.plusAssign(builder: suspend (Player) -> Unit) = values.forEach { it += builder }

        /**
         * When the player consumes an item in this collection, this potion will be applied.
         * #Note: The potions key will be overwritten.
         *
         * @param builder The builder to apply the potion.
         * @receiver The collection of foods.
         */
        operator fun MaterialSetTag.plusAssign(builder: (PotionEffectBuilder) -> Unit) = values.forEach { it += builder }

        /**
         * When the player consumes an item in this collection, this Timed Attribute will be applied.
         *
         * @param builder The builder to apply the Timed Attribute.
         * @receiver The collection of foods.
         */
        @JvmName("plusAssignTimedAttributeBuilder")
        operator fun MaterialSetTag.plusAssign(builder: (TimedAttributeBuilder) -> Unit) = values.forEach { it += builder }

        /**
         * When the player consumes this food, the granted food level will be multiplied by this value.
         * ### Note: If the food is not [Material.isEdible], this will do nothing.
         *
         * @param value The value to multiply the food level by.
         * @receiver The food.
         */
        operator fun Material.timesAssign(value: Number) { if (checkEdible()) foodMultipliers[this] = value.toDouble() }

        /**
         * When the player consumes this food, the granted food level will be divided by this value.
         * ### Note: If the food is not [Material.isEdible], this will do nothing.
         *
         * @param value The value to divide the food level by.
         * @receiver The food.
         */
        operator fun Material.divAssign(value: Number) { if (checkEdible()) foodMultipliers[this] = 1 / value.toDouble() }

        /**
         * When the player consumes this food, this lambda will be run async.
         * ### Note: If the food is not [Material.isEdible], this will do nothing.
         *
         * @param builder The lambda to run.
         * @receiver The food.
         */
        operator fun Material.plusAssign(builder: suspend (Player) -> Unit) { if (checkEdible()) foodBlocks[this] = builder }

        /**
         * When the player consumes this food, this potion will be applied.
         * ### Note: If the food is not [Material.isEdible], this will do nothing.
         *
         * @param builder The builder to apply the potion.
         * @receiver The food.
         */
        operator fun Material.plusAssign(builder: (PotionEffectBuilder) -> Unit) { if (checkEdible()) foodPotions.put(this, PotionEffectBuilder(builder).apply { foodKey(this@plusAssign) }.build()) }

        /**
         * When the player consumes this food, this Timed Attribute will be applied.
         * ### Note: If the food is not [Material.isEdible], this will do nothing.
         *
         * @param builder The builder to apply the Timed Attribute.
         * @receiver The food.
         */
        @JvmName("plusAssignTimedAttributeBuilder")
        operator fun Material.plusAssign(builder: (TimedAttributeBuilder) -> Unit) {
            if (checkEdible()) foodAttributes.put(this, TimedAttributeBuilder(builder).materialName(this, this@AbstractOrigin))
        }

        private fun Material.checkEdible(): Boolean {
            if (!isEdible) getKoin().get<Terix>().log.warn { "Trying to add a non-edible material to the food builder: $name" }
            return isEdible
        }
    }

    protected inner class AbilityBuilder {

        fun <T : AbstractAbility> KeyBinding.add(clazz: KClass<out T>) = abilities.put(this, OriginService.getAbility(clazz))

        inline fun <reified T : AbstractAbility> KeyBinding.add() = add(T::class)
    }

    protected inner class BiomeBuilder {

        operator fun <T : AbstractAbility> Biome.plusAssign(ability: KClass<out T>) {
        }
    }

    override fun toString(): String {
        return "Origin(name='$name', item=$item, " +
            "abilities=$abilities, triggerBlocks=$triggerBlocks, damageTicks=$damageTicks, " +
            "damageActions=$damageActions, foodPotions=$foodPotions, foodAttributes=$foodAttributes, foodMultipliers=$foodMultipliers)"
    }
}
