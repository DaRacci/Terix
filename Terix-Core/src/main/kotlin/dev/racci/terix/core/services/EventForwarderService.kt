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
import dev.racci.terix.core.extensions.origin
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
import org.spigotmc.event.entity.EntityDismountEvent
import org.spigotmc.event.entity.EntityMountEvent

@MappedExtension(Terix::class, "Event Forwarder Service", [OriginService::class])
class EventForwarderService(override val plugin: Terix) : Extension<Terix>() {

    // TODO -> HashMap reference for events to the KCallable
    // TODO -> Implement caching knowledge of if an origin should have the event forwarded.
    override suspend fun handleEnable() {
        event<PlayerOriginChangeEvent> {
            preOrigin.onChange(this)
            newOrigin.onChange(this)
        }
        event<PlayerRespawnEvent> { player.origin().onRespawn(this) }
        event<PlayerPostRespawnEvent> { player.origin().onPostRespawn(this) }
        event<PlayerDeathEvent> { player.origin().onDeath(this) }
        event<PlayerEnterLiquidEvent> { player.origin().onEnterLiquid(this) }
        event<PlayerExitLiquidEvent> { player.origin().onExitLiquid(this) }
        event<BlockBreakEvent> { player.origin().onBreakBlock(this) }
        event<BlockPlaceEvent> { player.origin().onPlaceBlock(this) }
        event<EntityDamageEvent> { (entity as? Player)?.origin()?.onDamage(this) }
        event<EntityDamageByEntityEvent> { (damager as? Player)?.origin()?.onDamageEntity(this) }
        event<EntityDamageByEntityEvent> { (entity as? Player)?.origin()?.onDamageByEntity(this) }
        event<EntityDeathEvent> { entity.killer?.origin()?.onKillEntity(this) }
        event<PlayerLaunchProjectileEvent> { player.origin().onProjectileLaunch(this) }
        event<ProjectileHitEvent> { (entity.shooter as? Player)?.origin()?.onProjectileLand(this) }
        event<ProjectileCollideEvent> { (entity.shooter as? Player)?.origin()?.onProjectileCollide(this) }
        event<PlayerArmorChangeEvent> { player.origin().onArmourChange(this) }
        event<PlayerChangedWorldEvent> { player.origin().onChangeWorld(this) }
        event<PlayerFishEvent> { player.origin().onFish(this) }
        event<PlayerItemDamageEvent> { player.origin().onItemDamage(this) }
        event<PlayerRiptideEvent> { player.origin().onRiptide(this) }
        event<EntityCombustEvent> { (entity as? Player)?.origin()?.onCombust(this) }
        event<EntityResurrectEvent> { (entity as? Player)?.origin()?.onResurrect(this) }
        event<EntityToggleSwimEvent> { (entity as? Player)?.origin()?.onToggleSwim(this) }
        event<EntityToggleGlideEvent> { (entity as? Player)?.origin()?.onToggleGlide(this) }
        event<PlayerJumpEvent> { player.origin().onJump(this) }
        event<EntityKnockbackByEntityEvent> { (entity as? Player)?.origin()?.onKnockback(this) }
        event<PhantomPreSpawnEvent> { (spawningEntity as? Player)?.origin()?.onPhantomSpawn(this) }
        event<PlayerElytraBoostEvent> { player.origin().onElytraBoost(this) }
        event<EntityMountEvent> { (entity as? Player)?.origin()?.onEntityMount(this) }
        event<EntityDismountEvent> { (entity as? Player)?.origin()?.onEntityDismount(this) }
        event<InventoryOpenEvent> { (player as? Player)?.origin()?.onInventoryOpen(this) }
        event<EntityAirChangeEvent> { (entity as? Player)?.origin()?.onAirChange(this) }
        event<PlayerBedEnterEvent> { player.origin().onEnterBed(this) }
        event<PlayerInteractEvent> { player.origin().onInteract(this) }
        event<PlayerItemConsumeEvent> { player.origin().onConsume(this) }

        // Combo Events
        event<PlayerLeftClickEvent> { player.origin().onLeftClick(this) }
        event<PlayerRightClickEvent> { player.origin().onRightClick(this) }
        event<PlayerOffhandEvent> { player.origin().onOffhand(this) }
        event<PlayerDoubleLeftClickEvent> { player.origin().onDoubleLeftClick(this) }
        event<PlayerDoubleRightClickEvent> { player.origin().onDoubleRightClick(this) }
        event<PlayerDoubleOffhandEvent> { player.origin().onDoubleOffhand(this) }
        event<PlayerShiftLeftClickEvent> { player.origin().onSneakLeftClick(this) }
        event<PlayerShiftRightClickEvent> { player.origin().onSneakRightClick(this) }
        event<PlayerShiftOffhandEvent> { player.origin().onSneakOffhand(this) }
        event<PlayerShiftDoubleLeftClickEvent> { player.origin().onSneakDoubleLeftClick(this) }
        event<PlayerShiftDoubleRightClickEvent> { player.origin().onSneakDoubleRightClick(this) }
        event<PlayerShiftDoubleOffhandEvent> { player.origin().onSneakDoubleOffhand(this) }
    }
}
