package dev.racci.terix.api.origins.origin

import dev.racci.terix.api.origins.origin.OriginValues.AbilityGenerator
import dev.racci.terix.api.origins.sounds.SoundEffect
import kotlinx.collections.immutable.PersistentSet
import net.kyori.adventure.text.format.TextColor
import net.minecraft.world.entity.player.Abilities
import org.bukkit.inventory.ItemStack

public data class OriginDataHolder internal constructor(
    public val information: Information,
    public val abilities: Abilities,
    public val sounds: Sounds
) {

    /**
     * Defines that this class is a global data class and holds no player / instance related data.
     */
    private annotation class GlobalDataHolder

    public data class Information internal constructor(
        val name: String,
        val colour: TextColor,
        val item: ItemStack
    ) { public companion object }

    @JvmInline
    public value class AbilityGenerators internal constructor(
        public val abilityGenerators: PersistentSet<AbilityGenerator<*>>
    ) { public companion object }

//    @optics([OpticsTarget.DSL, OpticsTarget.LENS]) public data class States internal constructor() {
//        public companion object
//    }

//    @optics([OpticsTarget.DSL, OpticsTarget.LENS]) public data class Effects internal constructor() {
//        public companion object
//    }

    public data class Sounds internal constructor(
        public val hurtSound: SoundEffect = SoundEffect("entity.player.hurt"),
        public val deathSound: SoundEffect = SoundEffect("entity.player.death"),
        public val ambientSound: SoundEffect? = null
    ) { public companion object }

//    @optics([OpticsTarget.DSL, OpticsTarget.LENS]) public data class Particles internal constructor() {
//        public companion object
//    }

//    @optics([OpticsTarget.DSL, OpticsTarget.LENS]) public data class Biomes internal constructor() {
//        public companion object
//    }

//    @optics([OpticsTarget.DSL, OpticsTarget.LENS]) public data class Titles internal constructor() {
//        public companion object
//    }

//    @optics([OpticsTarget.DSL, OpticsTarget.LENS])
//    public data class Actions internal constructor() {
//        public companion object
//    }

//    @optics([OpticsTarget.DSL, OpticsTarget.LENS])
//    public data class ReceivedDamageModification internal constructor() {
//        public companion object
//    }
}
