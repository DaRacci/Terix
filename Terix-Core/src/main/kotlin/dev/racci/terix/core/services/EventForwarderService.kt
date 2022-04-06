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
import dev.racci.minix.api.events.PlayerDoubleOffhandEvent
import dev.racci.minix.api.events.PlayerEnterLiquidEvent
import dev.racci.minix.api.events.PlayerExitLiquidEvent
import dev.racci.minix.api.events.PlayerShiftLeftClickEvent
import dev.racci.minix.api.events.PlayerShiftOffhandEvent
import dev.racci.minix.api.events.PlayerShiftRightClickEvent
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.event
import dev.racci.terix.api.Terix
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.core.extension.origin
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

@MappedExtension("Event Forwarder Service", [OriginService::class])
class EventForwarderService(override val plugin: Terix) : Extension<Terix>() {

    override suspend fun handleEnable() {
        event<PlayerOriginChangeEvent> { player.origin().onChange(this) }
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
        event<PlayerDoubleOffhandEvent> { player.origin().onDoubleOffhand(this) }
        event<PlayerShiftOffhandEvent> { player.origin().onShiftOffhand(this) }
        event<PlayerShiftLeftClickEvent> { player.origin().onShiftLeftClick(this) }
        event<PlayerShiftRightClickEvent> { player.origin().onShiftRightClick(this) }
        event<PlayerShiftOffhandEvent> { player.origin().onShiftOffhand(this) }
    }
}
