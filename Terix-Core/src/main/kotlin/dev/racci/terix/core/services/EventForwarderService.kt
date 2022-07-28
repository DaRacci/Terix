package dev.racci.terix.core.services

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent
import com.destroystokyo.paper.event.entity.PhantomPreSpawnEvent
import com.destroystokyo.paper.event.entity.ProjectileCollideEvent
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent
import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent
import com.destroystokyo.paper.event.player.PlayerJumpEvent
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import dev.racci.minix.api.annotations.MappedExtension
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
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.event
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.Terix
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.origin
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

// TODO: Only register when used by at-least one active origin.
// TODO: Create a cleaner function to register events.
// TODO -> HashMap reference for events to the KCallable
// TODO -> Implement caching knowledge of if an origin should have the event forwarded.
@MappedExtension(Terix::class, "Event Forwarder Service", [OriginService::class])
class EventForwarderService(override val plugin: Terix) : Extension<Terix>() {

    override suspend fun handleEnable() {
        event<PlayerOriginChangeEvent> {
            preOrigin.onChange(this)
            newOrigin.onChange(this)
        }
        event<PlayerRespawnEvent> { origin(player).onRespawn(this) }
        event<PlayerPostRespawnEvent> { origin(player).onPostRespawn(this) }
        event<PlayerDeathEvent> { origin(player).onDeath(this) }
        event<PlayerEnterLiquidEvent> { origin(player).onEnterLiquid(this) }
        event<PlayerExitLiquidEvent> { origin(player).onExitLiquid(this) }
        event<BlockBreakEvent> { origin(player).onBreakBlock(this) }
        event<BlockPlaceEvent> { origin(player).onPlaceBlock(this) }
        event<EntityDamageEvent> { (entity as? Player)?.let { origin(it) }?.onDamage(this) }
        event<EntityDamageByEntityEvent> { (damager as? Player)?.let { origin(it) }?.onDamageEntity(this) }
        event<EntityDamageByEntityEvent> { (entity as? Player)?.let { origin(it) }?.onDamageByEntity(this) }
        event<EntityDeathEvent> { entity.killer?.let { origin(it) }?.onKillEntity(this) }
        event<PlayerLaunchProjectileEvent> { origin(player).onProjectileLaunch(this) }
        event<ProjectileHitEvent> { (entity.shooter as? Player)?.let { origin(it) }?.onProjectileLand(this) }
        event<ProjectileCollideEvent> { (entity.shooter as? Player)?.let { origin(it) }?.onProjectileCollide(this) }
        event<PlayerArmorChangeEvent> { origin(player).onArmourChange(this) }
        event<PlayerChangedWorldEvent> { origin(player).onChangeWorld(this) }
        event<PlayerFishEvent> { origin(player).onFish(this) }
        event<PlayerItemDamageEvent> { origin(player).onItemDamage(this) }
        event<PlayerRiptideEvent> { origin(player).onRiptide(this) }
        event<EntityCombustEvent> { (entity as? Player)?.let { origin(it) }?.onCombust(this) }
        event<EntityResurrectEvent> { (entity as? Player)?.let { origin(it) }?.onResurrect(this) }
        event<EntityToggleSwimEvent> { (entity as? Player)?.let { origin(it) }?.onToggleSwim(this) }
        event<EntityToggleGlideEvent> { (entity as? Player)?.let { origin(it) }?.onToggleGlide(this) }
        event<PlayerJumpEvent> { origin(player).onJump(this) }
        event<EntityKnockbackByEntityEvent> { (entity as? Player)?.let { origin(it) }?.onKnockback(this) }
        event<PhantomPreSpawnEvent> { (spawningEntity as? Player)?.let { origin(it) }?.onPhantomSpawn(this) }
        event<PlayerElytraBoostEvent> { origin(player).onElytraBoost(this) }
        event<EntityMountEvent> { (entity as? Player)?.let { origin(it) }?.onEntityMount(this) }
        event<EntityDismountEvent> { (entity as? Player)?.let { origin(it) }?.onEntityDismount(this) }
        event<InventoryOpenEvent> { (player as? Player)?.let { origin(it) }?.onInventoryOpen(this) }
        event<EntityAirChangeEvent> { (entity as? Player)?.let { origin(it) }?.onAirChange(this) }
        event<PlayerBedEnterEvent> { origin(player).onEnterBed(this) }
        event<PlayerInteractEvent> { origin(player).onInteract(this) }
        event<PlayerItemConsumeEvent> { origin(player).onConsume(this) }
        event<PlayerToggleSneakEvent> { origin(player).onToggleSneak(this) }

        // Combo Events
        event<PlayerLeftClickEvent> { origin(player).onLeftClick(this) }
        event<PlayerRightClickEvent> { origin(player).onRightClick(this) }
        event<PlayerOffhandEvent> { origin(player).onOffhand(this) }
        event<PlayerDoubleLeftClickEvent> { origin(player).onDoubleLeftClick(this) }
        event<PlayerDoubleRightClickEvent> { origin(player).onDoubleRightClick(this) }
        event<PlayerDoubleOffhandEvent> { origin(player).onDoubleOffhand(this) }
        event<PlayerShiftLeftClickEvent> { origin(player).onSneakLeftClick(this) }
        event<PlayerShiftRightClickEvent> { origin(player).onSneakRightClick(this) }
        event<PlayerShiftOffhandEvent> { origin(player).onSneakOffhand(this) }
        event<PlayerShiftDoubleLeftClickEvent> { origin(player).onSneakDoubleLeftClick(this) }
        event<PlayerShiftDoubleRightClickEvent> { origin(player).onSneakDoubleRightClick(this) }
        event<PlayerShiftDoubleOffhandEvent> { origin(player).onSneakDoubleOffhand(this) }
    }
}
