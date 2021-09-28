package me.racci.sylphia.origins

import com.destroystokyo.paper.MaterialSetTag

// Attach an instance of each class to the origins with the specified settings

data class LifeSteal(val enabled: Boolean,
                     val percent: Int,
                     val cooldown: Int,
                     val tools: HashSet<MaterialSetTag>) {

}

data class SilkTouch(val enabled: Boolean,
                     val tools: HashSet<MaterialSetTag>) {

}

// General purpose summon data class

// Player speed up to ram entity

// Super jump

// Teleport with and without enderpearl // look into source code for enderman random teleport math

// Activate elytra without one equiped

// Levitation, and heal wounds

// Shoot fireball

// Suck blood / lifeforce

// Tidal wave

// Silk touch
