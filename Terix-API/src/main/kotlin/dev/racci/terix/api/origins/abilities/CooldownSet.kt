package dev.racci.terix.api.origins.abilities

import dev.racci.minix.api.extensions.onlinePlayers
import dev.racci.minix.api.extensions.scheduler
import dev.racci.minix.api.utils.now
import kotlinx.datetime.Instant
import org.bukkit.entity.Player
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class CooldownSet(private val ability: KeybindAbility) {
    // If Instant is null, cooldown is expired.
    // If Boolean is true, ability is active.
    private val cacheMap = hashMapOf<UUID, Pair<Boolean, Instant?>>()

    fun add(
        element: UUID,
        forceValue: Instant?
    ): Boolean {
        if (forceValue != null && forceValue < now()) {
            cacheMap[element] = true to forceValue
            return true
        }

        this.checkExpired(element)
        return cacheMap.computeIfAbsent(element) { true to now() }.first
    }

    fun remove(
        element: UUID,
        forceRemove: Boolean
    ): Instant? {
        if (forceRemove) {
            cacheMap.remove(element)
            return null
        }

        return this.cacheMap.computeIfPresent(element) { _, pair -> false to pair.second }?.second
    }

    fun containsCooldown(element: UUID): Boolean {
        this.checkExpired(element)
        return this.cacheMap[element]?.second != null
    }

    operator fun contains(element: UUID): Boolean {
        this.checkExpired(element)
        return this.cacheMap[element]?.first ?: false
    }

    operator fun get(element: UUID) = this.cacheMap[element]?.second
    operator fun plusAssign(element: UUID) {
        this.add(element, null)
    }

    operator fun minusAssign(element: UUID) {
        this.remove(element, false)
    }

    private fun checkExpired(element: UUID) {
        this.cacheMap.computeIfPresent(element) { _, (active, cooldown) ->
            val newCooldown = if (cooldown == null || cooldown + this.ability.cooldown < now()) {
                null
            } else cooldown

            when {
                active -> true to newCooldown
                newCooldown != null -> false to newCooldown
                else -> null
            }
        }
    }

    init {
        scheduler {
            if (onlinePlayers.isEmpty() || this.cacheMap.isEmpty()) return@scheduler

            val uuidList = onlinePlayers.map(Player::getUniqueId)
            for ((player) in this.cacheMap) {
                if (player in uuidList) continue

                this.remove(player, true)
            }
        }.runAsyncTaskTimer(ability.plugin, Duration.ZERO, 15.seconds)
    }
}
