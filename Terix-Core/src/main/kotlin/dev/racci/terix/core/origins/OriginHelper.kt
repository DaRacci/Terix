package dev.racci.terix.core.origins

import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.extensions.async
import dev.racci.minix.api.extensions.sync
import dev.racci.minix.api.utils.getKoin
import dev.racci.terix.api.Terix
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.origin
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.api.sentryUser
import dev.racci.terix.core.extensions.activeTriggers
import dev.racci.terix.core.extensions.fromOrigin
import dev.racci.terix.core.extensions.nightVision
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryLevel
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.time.Duration

object OriginHelper : WithPlugin<Terix> {
    override val plugin by getKoin().inject<Terix>()

    suspend fun add(
        player: Player,
        origin: AbstractOrigin,
        trigger: Trigger
    ) {
        sentryBreadcrumb(player, origin, trigger, "add")

        applyAsyncable(player, origin, trigger, false)
        applyMainThread(player, origin, trigger, false)
    }

    suspend fun swap(
        player: Player,
        origin: AbstractOrigin,
        lastTrigger: Trigger,
        nextTrigger: Trigger
    ) {
        sentryBreadcrumb(player, origin, nextTrigger, "swap_$lastTrigger")

        applyAsyncable(player, origin, lastTrigger, true)
        applyAsyncable(player, origin, nextTrigger, false)
        sync {
            applyMainThread(player, origin, lastTrigger, true)
            applyMainThread(player, origin, nextTrigger, false)
        }
    }

    suspend fun remove(
        player: Player,
        origin: AbstractOrigin,
        trigger: Trigger
    ) {
        sentryBreadcrumb(player, origin, trigger, "remove")

        applyAsyncable(player, origin, trigger, true)
        applyMainThread(player, origin, trigger, true)
    }

    suspend fun applyBase(
        player: Player,
        origin: AbstractOrigin = origin(player)
    ) {
        sentryBreadcrumb(player, origin, Trigger.ON, "apply_base")

        applyAsyncable(player, origin, Trigger.ON, false)
        applyMainThread(player, origin, Trigger.ON, false)

        player.setCanBreathUnderwater(origin.waterBreathing)
        player.setImmuneToFire(origin.fireImmune)
    }

    suspend fun changeTo(
        player: Player,
        oldOrigin: AbstractOrigin?,
        newOrigin: AbstractOrigin
    ) {
        sentryBreadcrumb(player, newOrigin, Trigger.ON, "reload")

        player.setImmuneToFire(newOrigin.fireImmune)
        player.setCanBreathUnderwater(newOrigin.waterBreathing)

        val activeTriggers = player.activeTriggers()
        val removePotions = arrayListOf<PotionEffectType>()
        val curHealth = player.health

        if (oldOrigin != null) {
            activeTriggers.forEach { applyAsyncable(player, oldOrigin, it, true) }
            activeTriggers.map { getAllPotions(player, it) }.forEach(removePotions::addAll)
        }

        sync {
            removePotions.forEach(player::removePotionEffect)
            activeTriggers.forEach { newOrigin.potions[it]?.let(player::addPotionEffects) }
            when {
                oldOrigin?.nightVision == true && !newOrigin.nightVision -> {
                    removePotions += PotionEffectType.NIGHT_VISION
                }
                oldOrigin?.nightVision != true && newOrigin.nightVision -> {
                    val trigger = activeTriggers.find { it == player.nightVision } ?: return@sync
                    player.addPotionEffect(nightVisionPotion(newOrigin, trigger))
                }
            }
        }

        activeTriggers.forEach { applyAsyncable(player, newOrigin, it, false) }
        if (player.health < curHealth) player.health = curHealth.coerceAtMost(player.maxHealth)
    }

    fun getAllPotions(
        player: Player,
        trigger: Trigger?
    ): Sequence<PotionEffectType> = player.activePotionEffects
        .asSequence()
        .mapNotNull { effect ->
            val match = PotionEffectBuilder.regex.find(effect.key?.asString().orEmpty()) ?: return@mapNotNull null
            if (!(match.groups["type"]?.value == "potion" && trigger != null && match.groups["trigger"]?.value == trigger.name.lowercase())) return@mapNotNull null
            effect.type
        }

    private suspend fun applyAsyncable(
        player: Player,
        origin: AbstractOrigin,
        trigger: Trigger,
        remove: Boolean
    ) = async {
        if (remove) {
            val modifiers = origin.attributeModifiers[trigger] ?: return@async
            for ((attribute, modifier) in modifiers) {
                sentryBreadcrumb(player, origin, trigger, "removeAsync.$attribute-=[name=${modifier.name},amount=${modifier.amount},operation=${modifier.operation}]")
                player.getAttribute(attribute)?.removeModifier(modifier)
            }
        }

        origin.titles[trigger]?.invoke(player)
        origin.triggerBlocks[trigger]?.invoke(player)
        origin.attributeModifiers[trigger]?.forEach { (attribute, modifier) ->
            sentryBreadcrumb(player, origin, trigger, "addAsync.$attribute-=[name=${modifier.name},amount=${modifier.amount},operation=${modifier.operation}]")
            player.getAttribute(attribute)?.addModifier(modifier)
        }
    }

    // TODO: Cache if the player has their nightvision potion
    private fun applyMainThread(
        player: Player,
        origin: AbstractOrigin,
        trigger: Trigger,
        remove: Boolean
    ) = sync {
        if (remove) {
            origin.potions[trigger]?.takeUnless(Collection<*>::isEmpty)?.map(PotionEffect::getType)?.forEach(player::removePotionEffect)
            if (origin.nightVision &&
                player.nightVision == trigger &&
                player.activePotionEffects.find { it.type == PotionEffectType.NIGHT_VISION }?.fromOrigin() == true
            ) player.removePotionEffect(PotionEffectType.NIGHT_VISION)
            return@sync
        }
        origin.potions[trigger]?.takeUnless(Collection<*>::isEmpty)?.let(player::addPotionEffects)
        if (origin.nightVision &&
            player.nightVision == trigger &&
            player.activePotionEffects.find { it.type == PotionEffectType.NIGHT_VISION }?.fromOrigin() != true
        ) { player.addPotionEffect(nightVisionPotion(origin, trigger)) }
    }

    private fun nightVisionPotion(
        origin: AbstractOrigin,
        trigger: Trigger
    ) = PotionEffectBuilder {
        type = PotionEffectType.NIGHT_VISION
        duration = Duration.INFINITE
        amplifier = 0
        ambient = true
        originKey(origin, trigger)
    }.build()

    private fun sentryBreadcrumb(
        player: Player,
        origin: AbstractOrigin,
        trigger: Trigger?,
        functionName: String,
        otherOrigin: AbstractOrigin? = null,
        otherTrigger: Trigger? = null
    ) {
        async {
            Sentry.setUser(player.sentryUser())

            val breadcrumb = Breadcrumb()
            breadcrumb.type = "trace"
            breadcrumb.category = "terix.origin.trigger"
            breadcrumb.level = SentryLevel.DEBUG

            breadcrumb.message = StringBuilder(origin.name).apply {
                if (otherOrigin != null) {
                    append('.')
                    append(functionName)
                    append('.')
                    append(otherOrigin.name)
                }

                if (trigger != null) {
                    if (otherOrigin != null && this.endsWith(otherOrigin.name)) {
                        this.append(" | ")
                    }

                    append(trigger.name)
                    append(".")
                    append(functionName)

                    if (otherTrigger != null) {
                        append('.')
                        append(otherTrigger.name)
                    }
                }
            }.toString()

            Sentry.addBreadcrumb(breadcrumb)
        }
    }
}

@Deprecated("Moved", ReplaceWith("dev.racci.terix.core.origins.OriginHelper.add(player, origin, this)"))
suspend fun Trigger.invokeAdd(
    player: Player,
    origin: AbstractOrigin = origin(player)
) = OriginHelper.add(player, origin, this)

@Deprecated("Moved", ReplaceWith("dev.racci.terix.core.origins.OriginHelper.swap(player, origin, oldTrigger, this)"))
suspend fun Trigger.invokeSwap(
    oldTrigger: Trigger,
    player: Player,
    origin: AbstractOrigin = origin(player)
) = OriginHelper.swap(player, origin, oldTrigger, this)

@Deprecated("Moved", ReplaceWith("dev.racci.terix.core.origins.OriginHelper.remove(player, origin, this)"))
suspend fun Trigger.invokeRemove(
    player: Player,
    origin: AbstractOrigin = origin(player)
) = OriginHelper.remove(player, origin, this)

@Deprecated("Moved", ReplaceWith("dev.racci.terix.core.origins.OriginHelper.applyBase(player, origin)"))
suspend fun Trigger.Companion.invokeBase(
    player: Player,
    origin: AbstractOrigin = origin(player)
) = OriginHelper.applyBase(player, origin)

@Deprecated("Moved", ReplaceWith("dev.racci.terix.core.origins.OriginHelper.changeTo(player, oldOrigin, origin)"))
suspend fun Trigger.Companion.invokeReload(
    player: Player,
    oldOrigin: AbstractOrigin? = null,
    newOrigin: AbstractOrigin = origin(player)
) = OriginHelper.changeTo(player, oldOrigin, newOrigin)

@Deprecated("Moved", ReplaceWith("dev.racci.terix.core.origins.OriginHelper.getAllPotions(this, trigger)"))
fun Player.getAllOriginPotions(trigger: Trigger? = null): Collection<PotionEffectType> = OriginHelper.getAllPotions(this, trigger).toList()
