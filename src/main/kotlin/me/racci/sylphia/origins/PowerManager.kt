package me.racci.sylphia.origins

import com.destroystokyo.paper.MaterialSetTag

data class LifeSteal(val enabled: Boolean,
                     val percent: Int,
                     val cooldown: Int,
                     val tools: HashSet<MaterialSetTag>) {

}