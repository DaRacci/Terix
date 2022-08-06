package dev.racci.terix.api.origins.origin

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent
import com.destroystokyo.paper.event.entity.PhantomPreSpawnEvent
import com.destroystokyo.paper.event.entity.ProjectileCollideEvent
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent
import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent
import com.destroystokyo.paper.event.player.PlayerJumpEvent
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
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
import dev.racci.minix.api.extensions.KListener
import dev.racci.minix.api.plugin.MinixPlugin
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import org.apiguardian.api.API
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityAirChangeEvent
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.event.entity.EntityDamageByBlockEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityResurrectEvent
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.event.entity.EntityToggleSwimEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerBedLeaveEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerRiptideEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.spigotmc.event.entity.EntityDismountEvent
import org.spigotmc.event.entity.EntityMountEvent
import org.spigotmc.event.player.PlayerSpawnLocationEvent

/** Provides all supported events for origins to listen to. */
@API(status = API.Status.STABLE, since = "1.0.0")
sealed interface OriginEventListener : KListener<MinixPlugin> {

    /** Called when Terix first registers this origin. */
    suspend fun onRegister() = Unit

    /** Called when the player respawns. */
    suspend fun onRespawn(event: PlayerRespawnEvent) = Unit

    /** Called when the player first becomes this origin. */
    suspend fun onBecomeOrigin(event: PlayerOriginChangeEvent) = Unit

    /** Called when the player changes to another origin. */
    suspend fun onChangeOrigin(event: PlayerOriginChangeEvent) = Unit

    /** Called after the player respawns. */
    suspend fun onPostRespawn(event: PlayerPostRespawnEvent) = Unit

    /** Called when the player dies. */
    suspend fun onDeath(event: PlayerDeathEvent) = Unit

    /** Called when the player enters a liquid. */
    suspend fun onEnterLiquid(event: PlayerEnterLiquidEvent) = Unit

    /** Called when the player exits a liquid. */
    suspend fun onExitLiquid(event: PlayerExitLiquidEvent) = Unit

    /** Called when the player breaks a block. */
    suspend fun onBreakBlock(event: BlockBreakEvent) = Unit

    /** Called when the player places a block. */
    suspend fun onPlaceBlock(event: BlockPlaceEvent) = Unit

    /** Called when the player is damaged by a non-entity source. */
    suspend fun onDamage(event: EntityDamageEvent) = Unit

    /** Called when the player damages another entity. */
    suspend fun onDamageEntity(event: EntityDamageByEntityEvent) = Unit

    /** Called when the player is damaged by a block. */
    suspend fun onDamageByBlock(event: EntityDamageByBlockEvent) = Unit

    /** Called when the player is damaged by another entity. */
    suspend fun onDamageByEntity(event: EntityDamageByEntityEvent) = Unit

    /** Called when the player kills another entity. */
    suspend fun onKillEntity(event: EntityDeathEvent) = Unit

    /** Called when the player launches a projectile. */
    suspend fun onProjectileLaunch(event: PlayerLaunchProjectileEvent) = Unit

    /** Called when the player's projectile lands. */
    suspend fun onProjectileHit(event: ProjectileHitEvent) = Unit

    /** Called when the player's projectile hits an entity. */
    suspend fun onProjectileCollide(event: ProjectileCollideEvent) = Unit

    /** Called when the player changes their armor. */
    suspend fun onArmourChange(event: PlayerArmorChangeEvent) = Unit

    /** Called when the player changes worlds. */
    suspend fun onChangedWorld(event: PlayerChangedWorldEvent) = Unit

    /** Called when the player fishes. */
    suspend fun onFish(event: PlayerFishEvent) = Unit

    /** Called when the player damages an item. */
    suspend fun onItemDamage(event: PlayerItemDamageEvent) = Unit

    /** Called when the player uses riptide on a trident. */
    suspend fun onRiptide(event: PlayerRiptideEvent) = Unit

    /** Called when the player starts to burn. */
    suspend fun onCombust(event: EntityCombustEvent) = Unit

    /** Called when the player is resurrected by a totem. */
    suspend fun onResurrect(event: EntityResurrectEvent) = Unit

    /** Called when the player enters the swimming animation. */
    suspend fun onToggleSwim(event: EntityToggleSwimEvent) = Unit

    /** Called when the player enters the gliding animation. */
    suspend fun onToggleGlide(event: EntityToggleGlideEvent) = Unit

    /** Called when the player jumps. */
    suspend fun onJump(event: PlayerJumpEvent) = Unit

    /** Called when the player is knocked back by another entity. */
    suspend fun onKnockbackByEntity(event: EntityKnockbackByEntityEvent) = Unit

    /** Called when the player knocks back another entity. */
    suspend fun onKnockbackEntity(event: EntityKnockbackByEntityEvent) = Unit

    /** Called when a phantom will spawn due to a player's insomnia. */
    suspend fun onPhantomSpawn(event: PhantomPreSpawnEvent) = Unit

    /** Called when the player boosts an elytra. */
    suspend fun onElytraBoost(event: PlayerElytraBoostEvent) = Unit

    /** Called when the player mounts another entity. */
    suspend fun onMount(event: EntityMountEvent) = Unit

    /** Called when the player dismounts another entity. */
    suspend fun onDismount(event: EntityDismountEvent) = Unit

    /** Called when the player left-clicks. */
    suspend fun onLeftClick(event: PlayerLeftClickEvent) = Unit

    /** Called when the player right-clicks. */
    suspend fun onRightClick(event: PlayerRightClickEvent) = Unit

    /** Called when the player uses their offhand key. */
    suspend fun onOffhand(event: PlayerOffhandEvent) = Unit

    /** Called when the player double-clicks the left-mouse button. */
    suspend fun onDoubleLeftClick(event: PlayerDoubleLeftClickEvent) = Unit

    /** Called when the player double-clicks the right-mouse button. */
    suspend fun onDoubleRightClick(event: PlayerDoubleRightClickEvent) = Unit

    /** Called when the player double-clicks the offhand key. */
    suspend fun onDoubleOffhand(event: PlayerDoubleOffhandEvent) = Unit

    /** Called when the player left-clicks while sneaking. */
    suspend fun onSneakLeftClick(event: PlayerShiftLeftClickEvent) = Unit

    /** Called when the player right-clicks while sneaking. */
    suspend fun onSneakRightClick(event: PlayerShiftRightClickEvent) = Unit

    /** Called when the player uses the offhand key while sneaking. */
    suspend fun onSneakOffhand(event: PlayerShiftOffhandEvent) = Unit

    /** Called when the player double left-clicks while sneaking. */
    suspend fun onSneakDoubleLeftClick(event: PlayerShiftDoubleLeftClickEvent) = Unit

    /** Called when the player double right-clicks while sneaking. */
    suspend fun onSneakDoubleRightClick(event: PlayerShiftDoubleRightClickEvent) = Unit

    /** Called when the player double uses the offhand key while sneaking. */
    suspend fun onSneakDoubleOffhand(event: PlayerShiftDoubleOffhandEvent) = Unit

    /** Called when the player opens an inventory. */
    suspend fun onInventoryOpen(event: InventoryOpenEvent) = Unit

    /** Called when the player's remaining air changes. */
    suspend fun onAirChange(event: EntityAirChangeEvent) = Unit

    /** Called when the player enters a bed. */
    suspend fun onBedEnter(event: PlayerBedEnterEvent) = Unit

    /** Called when the player leaves a bed. */
    suspend fun onBedLeave(event: PlayerBedLeaveEvent) = Unit

    /** Called when the player interacts with something. */
    suspend fun onInteract(event: PlayerInteractEvent) = Unit

    /** Called when the player consumes an item. */
    suspend fun onItemConsume(event: PlayerItemConsumeEvent) = Unit

    /** Called when the player toggles sneaking. */
    suspend fun onToggleSneak(event: PlayerToggleSneakEvent) = Unit

    /** Called when the player spawn location is being found. */
    suspend fun onSpawnLocation(event: PlayerSpawnLocationEvent) = Unit

    /** Called each game tick. */
    suspend fun onTick(player: Player) = Unit
}
