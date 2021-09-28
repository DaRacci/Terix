package me.racci.sylphia.listeners

import me.racci.raccicore.skedule.skeduleAsync
import me.racci.raccicore.skedule.skeduleSync
import me.racci.sylphia.Sylphia
import me.racci.sylphia.extensions.PlayerExtension.currentOrigin
import me.racci.sylphia.extensions.PlayerExtension.isDay
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.event.player.PlayerJoinEvent

class LoopListener(private val plugin: Sylphia): Listener {




    @EventHandler(priority = EventPriority.HIGHEST)
    fun onCombustEvent(event: EntityCombustEvent) {
        if (event.entity is Player) {
            val origin = (event.entity as Player).currentOrigin ?: return
            if (origin.enableLava && origin.lavaAmount == 0) event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onJoin(event: PlayerJoinEvent) {
        skeduleAsync(plugin) {
            waitFor(40)
            startLoop(event.player)
        }
    }

    private fun startLoop(player: Player) {
        val origin = player.currentOrigin ?: return
        if (origin.enableRain && origin.enableWater && origin.enableSun) {
            println("Enable All")
            val rainAmount = 1 * (origin.rainAmount / 100).toDouble()
            val waterAmount = 2 * (origin.sunAmount / 100).toDouble()
            val sunAmount = 2 * (origin.sunAmount / 100).toDouble()
            skeduleAsync(plugin) {
                repeating(20)
                while (player.isOnline) {
                    if (player.isInWaterOrBubbleColumn) {
                        skeduleSync(plugin) {
                            player.damage(waterAmount)
                        }; yield()
                    } else if (player.isInRain) {
                        skeduleSync(plugin) {
                            player.damage(rainAmount)
                        }; yield()
                    } else {
                        if (player.isDay && player.isInWaterOrRainOrBubbleColumn && player.location.block.lightFromSky.toInt() == 15) {
                            player.isVisualFire = true
                            skeduleSync(plugin) {
                                player.damage(sunAmount)
                                player.world.playSound(player.location, Sound.ENTITY_GENERIC_BURN, 1f, 1f)
                            }; yield()
                        } else player.isVisualFire = false
                    }; yield()
                }; currentTask?.cancel()
            }
        } else if (origin.enableRain && origin.enableWater) {
            println("Enable Rain + Water")
            val rainAmount = 1 * (origin.rainAmount / 100).toDouble()
            val waterAmount = 2 * (origin.sunAmount / 100).toDouble()
            skeduleAsync(plugin) {
                repeating(20)
                while (player.isOnline) {
                    if (player.isInWaterOrBubbleColumn) {
                        skeduleSync(plugin) {
                            player.damage(waterAmount)
                        }; yield()
                    } else if (player.isInRain) {
                        skeduleSync(plugin) {
                            player.damage(rainAmount)
                        }; yield()
                    }; yield()
                }; currentTask?.cancel()
            }
        } else if (origin.enableSun && origin.enableRain) {
            println("Enable Sun + Rain")
            val rainAmount = 1 * (origin.rainAmount / 100).toDouble()
            val sunAmount = 2 * (origin.sunAmount / 100).toDouble()
            skeduleAsync(plugin) {
                repeating(20)
                while (player.isOnline) {
                    if (player.isInRain) {
                        skeduleSync(plugin) {
                            player.damage(rainAmount)
                        }; yield()
                    } else {
                        if (player.isDay && player.isInWaterOrRainOrBubbleColumn && player.location.block.lightFromSky.toInt() == 15) {
                            player.isVisualFire = true
                            skeduleSync(plugin) {
                                player.damage(sunAmount)
                                player.world.playSound(player.location, Sound.ENTITY_GENERIC_BURN, 1f, 1f)
                            }; yield()
                        } else player.isVisualFire = false
                    }; yield()
                }; currentTask?.cancel()
            }
        } else if (origin.enableSun && origin.enableWater) {
            println("Enable Sun + Water")
            val waterAmount = 2 * (origin.waterAmount / 100).toDouble()
            val sunAmount = 2 * (origin.sunAmount / 100).toDouble()
            skeduleAsync(plugin) {
                repeating(20)
                while (player.isOnline) {
                    if (player.isInWaterOrBubbleColumn) {
                        skeduleSync(plugin) {
                            player.damage(waterAmount)
                        }; yield()
                    } else {
                        if (player.isDay && player.isInWaterOrRainOrBubbleColumn && player.location.block.lightFromSky.toInt() == 15) {
                            player.isVisualFire = true
                            skeduleSync(plugin) {
                                player.damage(sunAmount)
                                player.world.playSound(player.location, Sound.ENTITY_GENERIC_BURN, 1f, 1f)
                            }; yield()
                        } else player.isVisualFire = false
                    }; yield()
                }; currentTask?.cancel()
            }
        } else {
            if (origin.enableSun) {
                println("Enable Sun")
                val sunAmount = 2 * (origin.sunAmount / 100).toDouble()
                skeduleAsync(plugin) {
                    repeating(20)
                    while (player.isOnline) {
                        if (player.isDay && player.isInWaterOrRainOrBubbleColumn && player.location.block.lightFromSky.toInt() == 15) {
                            player.isVisualFire = true
                            skeduleSync(plugin) {
                                player.damage(sunAmount)
                                player.world.playSound(player.location, Sound.ENTITY_GENERIC_BURN, 1f, 1f)
                            }
                        } else player.isVisualFire = false; yield()
                    }; currentTask?.cancel()
                }
            }
            if (origin.enableRain) {
                println("Enable Rain")
                val rainAmount = 1 * (origin.rainAmount / 100).toDouble()
                skeduleAsync(plugin) {
                    repeating(20)
                    while (player.isOnline) {
                        if (player.isInRain) {
                            skeduleSync(plugin) {
                                player.damage(rainAmount)
                            }
                        }; yield()
                    }; currentTask?.cancel()
                }
            }
            if (origin.enableWater) {
                println("Enable Water")
                val waterAmount = 2 * (origin.waterAmount / 100).toDouble()
                skeduleAsync(plugin) {
                    repeating(20)
                    while (player.isOnline) {
                        if (player.isInWaterOrBubbleColumn) {
                            skeduleSync(plugin) {
                                player.damage(waterAmount)
                            }
                        }; yield()
                    }; currentTask?.cancel()
                }
            }
        }
    }

    private fun burnInSun(player: Player) : Boolean {
        return player.isDay && !player.isInWaterOrRainOrBubbleColumn && player.location.block.temperature > 0.0 && player.location.block.lightFromSky.toInt() == 15
    }
    private fun damageInRain(player: Player) : Boolean {
        return player.isInRain && player.location.block.temperature in 0.01..1.0
    }

}
