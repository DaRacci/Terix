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
import dev.racci.minix.api.events.PlayerDoubleOffhandEvent
import dev.racci.minix.api.events.PlayerEnterLiquidEvent
import dev.racci.minix.api.events.PlayerExitLiquidEvent
import dev.racci.minix.api.events.PlayerShiftDoubleOffhandEvent
import dev.racci.minix.api.events.PlayerShiftLeftClickEvent
import dev.racci.minix.api.events.PlayerShiftOffhandEvent
import dev.racci.minix.api.events.PlayerShiftRightClickEvent
import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.plugin.MinixPlugin
import dev.racci.terix.api.dsl.AttributeModifierBuilder
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.dsl.TimedAttributeBuilder
import dev.racci.terix.api.dsl.TitleBuilder
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.origins.enums.Trigger
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.attribute.Attribute
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
import org.spigotmc.event.entity.EntityDismountEvent
import org.spigotmc.event.entity.EntityMountEvent

sealed interface IAbstractOrigin : WithPlugin<MinixPlugin> {

    val name: String
    val colour: TextColor
    val displayName: Component
    val hurtSound: Key
    val deathSound: Key

    val nightVision: Boolean
    val waterBreathing: Boolean
    val fireImmune: Boolean

    val becomeOriginTitle: TitleBuilder?

    suspend fun onRegister()

    /**
     * Called when the player respawns
     */
    suspend fun onRespawn(event: PlayerRespawnEvent) {}

    /**
     * Called when the player changes origins
     * Any special effects or caches should be cleared here.
     */
    suspend fun onChange(event: PlayerOriginChangeEvent) {}

    /**
     * Called after the player has respawned
     */
    suspend fun onPostRespawn(event: PlayerPostRespawnEvent) {}

    /**
     * Called on the players death
     */
    suspend fun onDeath(event: PlayerDeathEvent) {}

    /**
     * Called when the player enters a liquid block
     */
    suspend fun onEnterLiquid(event: PlayerEnterLiquidEvent) {}

    /**
     * Called when the player exits a liquid block
     */
    suspend fun onExitLiquid(event: PlayerExitLiquidEvent) {}

    /**
     * Called when the players breaks a block
     */
    suspend fun onBreakBlock(event: BlockBreakEvent) {}

    /**
     * Call when the player places a block
     */
    suspend fun onPlaceBlock(event: BlockPlaceEvent) {}

    /**
     * Called when the player takes damage.
     */
    suspend fun onDamage(event: EntityDamageEvent) {}

    /**
     * Called when the player damages another entity
     */
    suspend fun onDamageEntity(event: EntityDamageByEntityEvent) {}

    /**
     * Called when the player is damaged by an entity
     */
    suspend fun onDamageByEntity(event: EntityDamageByEntityEvent) {}

    /**
     * Called when the player kills an entity
     */
    suspend fun onKillEntity(event: EntityDeathEvent) {}

    /**
     * Called when the players launches a projectile
     */
    suspend fun onProjectileLaunch(event: PlayerLaunchProjectileEvent) {}

    /**
     * Called when a players projectile lands
     */
    suspend fun onProjectileLand(event: ProjectileHitEvent) {}

    suspend fun onProjectileCollide(event: ProjectileCollideEvent) {}

    suspend fun onArmourChange(event: PlayerArmorChangeEvent) {}

    suspend fun onChangeWorld(event: PlayerChangedWorldEvent) {}

    suspend fun onFish(event: PlayerFishEvent) {}

    suspend fun onItemDamage(event: PlayerItemDamageEvent) {}

    suspend fun onRiptide(event: PlayerRiptideEvent) {}

    suspend fun onCombust(event: EntityCombustEvent) {}

    suspend fun onResurrect(event: EntityResurrectEvent) {}

    suspend fun onToggleSwim(event: EntityToggleSwimEvent) {}

    suspend fun onToggleGlide(event: EntityToggleGlideEvent) {}

    suspend fun onJump(event: PlayerJumpEvent) {}

    suspend fun onKnockback(event: EntityKnockbackByEntityEvent) {}

    suspend fun onPhantomSpawn(event: PhantomPreSpawnEvent) {}

    suspend fun onElytraBoost(event: PlayerElytraBoostEvent) {}

    suspend fun onEntityMount(event: EntityMountEvent) {}

    suspend fun onEntityDismount(event: EntityDismountEvent) {}

    suspend fun onDoubleOffhand(event: PlayerDoubleOffhandEvent) {}

    suspend fun onShiftDoubleOffhand(event: PlayerShiftDoubleOffhandEvent) {}

    suspend fun onShiftOffhand(event: PlayerShiftOffhandEvent) {}

    suspend fun onShiftLeftClick(event: PlayerShiftLeftClickEvent) {}

    suspend fun onShiftRightClick(event: PlayerShiftRightClickEvent) {}

    interface PotionsBuilder {

        /**
         * Adds a potion effect to this trigger.
         * ## Note: The key of the potion effect will always be overridden.
         */
        infix fun Trigger.causes(builder: PotionEffectBuilder.() -> Unit)
    }

    interface AttributeBuilder {

        /**
         * Sets the base values of this origins attributes
         */
        infix fun Attribute.setBase(builder: AttributeModifierBuilder.() -> Unit)

        /**
         * Adds an attributeModifier to this trigger.
         */
        infix fun Trigger.causes(builder: AttributeModifierBuilder.() -> Unit)
    }

    interface TimeTitleBuilder {

        /**
         * Triggers a title when this trigger is activated.
         */
        infix fun Trigger.causes(builder: TitleBuilder.() -> Unit)
    }

    interface DamageBuilder {

        /**
         * When the player is damage and one of these triggers is satisfied the
         * event will be passed to the block for modification.
         */
        infix fun Trigger.invokes(builder: suspend (Player) -> Unit)

        /**
         * Damages the player by this amount when this trigger happens.
         * Most useful for triggers such as water, sunlight and darkness which
         * are called once per second.
         */
        infix fun Trigger.ticks(damage: Double)

        /**
         * Multiply the damage dealt to the player with this multiplier.
         * Setting this to 0 will cancel the event.
         */
        infix fun EntityDamageEvent.DamageCause.multiplied(multiplier: Double)

        /**
         * The same as [multiplied] but sets it for all items within the collection.
         */
        infix fun Collection<EntityDamageEvent.DamageCause>.multiplied(multiplier: Double)

        infix fun EntityDamageEvent.DamageCause.triggers(action: suspend EntityDamageEvent.() -> Unit)
    }

    interface FoodBuilder {

        infix fun MaterialSetTag.effects(builder: PotionEffectBuilder.() -> Unit)

        infix fun MaterialSetTag.applies(builder: TimedAttributeBuilder.() -> Unit)

        infix fun MaterialSetTag.multiplied(multiplier: Int)

        infix fun Material.effects(builder: PotionEffectBuilder.() -> Unit)

        infix fun Material.applies(builder: TimedAttributeBuilder.() -> Unit)

        infix fun Material.multiplied(value: Int)
    }

    interface ItemBuilder {

        infix fun AbstractOrigin.named(component: Component)

        infix fun AbstractOrigin.material(material: Material)

        infix fun AbstractOrigin.lore(builder: MutableMap<Int, Component>.() -> Unit)
    }
}
