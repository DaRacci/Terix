package me.racci.sylphia.events

import me.racci.raccicore.events.KotlinEvent
import me.racci.sylphia.origins.Origin
import org.bukkit.entity.Player

class OriginChangeEvent(val player: Player,
                        val oldOrigin: Origin?,
                        val newOrigin: Origin,
                        val sender: Player?,
                        ) : KotlinEvent(true) { }

class OriginResetEvent(val player: Player,
                       val sender: Player,
                       ) : KotlinEvent(true) { }

class BurnInSunLightEvent(val player: Player,
                          val damage: Double,
                          ) : KotlinEvent(false) { }

class RainDamageEvent(val player: Player,
                      val damage: Double,
                      ) : KotlinEvent(false) { }

class WaterDamageEvent(val player: Player,
                       val damage: Double,
                       ) : KotlinEvent(false) { }