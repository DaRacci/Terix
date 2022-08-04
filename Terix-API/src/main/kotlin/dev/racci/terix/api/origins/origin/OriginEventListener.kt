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

    suspend fun onRegister() = Unit

    suspend fun onRespawn(event: PlayerRespawnEvent) = Unit

    suspend fun onBecomeOrigin(event: PlayerOriginChangeEvent) = Unit

    suspend fun onChange(event: PlayerOriginChangeEvent) = Unit

    suspend fun onPostRespawn(event: PlayerPostRespawnEvent) = Unit

    suspend fun onDeath(event: PlayerDeathEvent) = Unit

    suspend fun onEnterLiquid(event: PlayerEnterLiquidEvent) = Unit

    suspend fun onExitLiquid(event: PlayerExitLiquidEvent) = Unit

    suspend fun onBreakBlock(event: BlockBreakEvent) = Unit

    suspend fun onPlaceBlock(event: BlockPlaceEvent) = Unit

    suspend fun onDamage(event: EntityDamageEvent) = Unit

    suspend fun onDamageEntity(event: EntityDamageByEntityEvent) = Unit

    suspend fun onDamageByEntity(event: EntityDamageByEntityEvent) = Unit

    suspend fun onKillEntity(event: EntityDeathEvent) = Unit

    suspend fun onProjectileLaunch(event: PlayerLaunchProjectileEvent) = Unit

    suspend fun onProjectileHit(event: ProjectileHitEvent) = Unit

    suspend fun onProjectileCollide(event: ProjectileCollideEvent) = Unit

    suspend fun onArmourChange(event: PlayerArmorChangeEvent) = Unit

    suspend fun onChangedWorld(event: PlayerChangedWorldEvent) = Unit

    suspend fun onFish(event: PlayerFishEvent) = Unit

    suspend fun onItemDamage(event: PlayerItemDamageEvent) = Unit

    suspend fun onRiptide(event: PlayerRiptideEvent) = Unit

    suspend fun onCombust(event: EntityCombustEvent) = Unit

    suspend fun onResurrect(event: EntityResurrectEvent) = Unit

    suspend fun onToggleSwim(event: EntityToggleSwimEvent) = Unit

    suspend fun onToggleGlide(event: EntityToggleGlideEvent) = Unit

    suspend fun onJump(event: PlayerJumpEvent) = Unit

    suspend fun onKnockbackByEntity(event: EntityKnockbackByEntityEvent) = Unit

    suspend fun onKnockbackEntity(event: EntityKnockbackByEntityEvent) = Unit

    suspend fun onPhantomSpawn(event: PhantomPreSpawnEvent) = Unit

    suspend fun onElytraBoost(event: PlayerElytraBoostEvent) = Unit

    suspend fun onMount(event: EntityMountEvent) = Unit

    suspend fun onDismount(event: EntityDismountEvent) = Unit

    suspend fun onLeftClick(event: PlayerLeftClickEvent) = Unit

    suspend fun onRightClick(event: PlayerRightClickEvent) = Unit

    suspend fun onOffhand(event: PlayerOffhandEvent) = Unit

    suspend fun onDoubleLeftClick(event: PlayerDoubleLeftClickEvent) = Unit

    suspend fun onDoubleRightClick(event: PlayerDoubleRightClickEvent) = Unit

    suspend fun onDoubleOffhand(event: PlayerDoubleOffhandEvent) = Unit

    suspend fun onSneakLeftClick(event: PlayerShiftLeftClickEvent) = Unit

    suspend fun onSneakRightClick(event: PlayerShiftRightClickEvent) = Unit

    suspend fun onSneakOffhand(event: PlayerShiftOffhandEvent) = Unit

    suspend fun onSneakDoubleLeftClick(event: PlayerShiftDoubleLeftClickEvent) = Unit

    suspend fun onSneakDoubleRightClick(event: PlayerShiftDoubleRightClickEvent) = Unit

    suspend fun onSneakDoubleOffhand(event: PlayerShiftDoubleOffhandEvent) = Unit

    suspend fun onInventoryOpen(event: InventoryOpenEvent) = Unit

    suspend fun onAirChange(event: EntityAirChangeEvent) = Unit

    suspend fun onBedEnter(event: PlayerBedEnterEvent) = Unit

    suspend fun onBedLeave(event: PlayerBedLeaveEvent) = Unit

    suspend fun onInteract(event: PlayerInteractEvent) = Unit

    suspend fun onItemConsume(event: PlayerItemConsumeEvent) = Unit

    suspend fun onToggleSneak(event: PlayerToggleSneakEvent) = Unit

    suspend fun onSpawnLocation(event: PlayerSpawnLocationEvent) = Unit

    suspend fun onTick(player: Player) = Unit
}
