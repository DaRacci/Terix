package dev.racci.terix.api.data

import com.destroystokyo.paper.Namespaced
import com.github.benmanes.caffeine.cache.Caffeine
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.dsl.AttributeModifierBuilder
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.origins.abilities.Ability
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.origin.OriginValues
import dev.racci.terix.api.origins.states.State
import org.bukkit.Material
import org.bukkit.NamespacedKey
import java.util.concurrent.TimeUnit

public data class OriginNamespacedTag private constructor(
    public val origin: String,
    public val source: Source,
    public val causeType: CauseType,
    public val cause: String
) : Namespaced {
    private val constructedKey: String by lazy {
        buildString {
            append(origin)
            append('/')
            append(source)
            append('/')
            append(causeType).append("__").append(cause)
        }
    }

    public val asString: String get() = "$namespace:$key"

    public val bukkitKey: NamespacedKey
        get() = tagCache[this, { NamespacedKey(namespace, key) }]

    public val isSourceBase: Boolean get() = source == Source.BASE
    public val isSourceAbility: Boolean get() = source == Source.ABILITY
    public val isSourceFood: Boolean get() = source == Source.FOOD

    public val isCauseTypeState: Boolean get() = causeType == CauseType.STATE
    public val isCauseTypeMaterial: Boolean get() = causeType == CauseType.MATERIAL
    public val isCauseTypeCustom: Boolean get() = causeType == CauseType.CUSTOM

    override fun getNamespace(): String {
        return "terix"
    }

    override fun getKey(): String {
        return "origin__$constructedKey"
    }

    public fun fromOrigin(origin: Origin): Boolean = this.origin == origin.name.lowercase()

    public fun fromState(state: State): Boolean = isCauseTypeState && this.cause == state.name.lowercase()

    public fun fromMaterial(material: Material): Boolean = isCauseTypeMaterial && this.cause == material.name.lowercase()

    public fun fromCustom(custom: String): Boolean = isCauseTypeCustom && this.cause == custom.lowercase()

    public fun getOrigin(): Origin = OriginService.getOrigin(origin)

    public fun getState(): State? = if (isCauseTypeState) State.valueOf(cause.uppercase()) else null

    public enum class Source { BASE, ABILITY, FOOD; }

    public enum class CauseType { STATE, SIDE_EFFECT, MATERIAL, CUSTOM; }

    public companion object {
        private val tagCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterAccess(5, TimeUnit.SECONDS)
            .build<OriginNamespacedTag, NamespacedKey>()

        /**
         * Match examples:
         *
         * terix:origin__angel/ability/custom__waterbonus
         *
         * terix:origin__slime/base/state__constant
         *
         * terix:origin__vampire/food/custom__apple
         */
        internal val REGEX: Regex = buildString {
            append("^terix:origin__")
            append("(?<origin>[a-z]+)")
            append('/')
            append("(?<source>").append(regexPart<Source>()).append(')')
            append('/')
            append("(?<type>").append(regexPart<CauseType>()).append(')').append("__").append("(?<cause>[a-z_-]+)")
            append("$")
        }.let(::Regex)

        private inline fun <reified E : Enum<E>> regexPart(): String = buildString {
            append("(?<${E::class.simpleName?.lowercase()}>")
            with(enumValues<E>()) {
                this.forEachIndexed { index, value ->
                    append(value.name.lowercase())
                    if (index != lastIndex) append("|")
                }
            }
            append(")")
        }

        private fun directGet(string: String): OriginNamespacedTag? {
            val match = REGEX.matchEntire(string) ?: return null
            val origin = match.groups["origin"]?.value ?: return null
            val source = match.groups["source"]?.value?.let { Source.valueOf(it.uppercase()) } ?: return null
            val type = match.groups["type"]?.value?.let { CauseType.valueOf(it.uppercase()) } ?: return null
            val cause = match.groups["cause"]?.value ?: return null

            return OriginNamespacedTag(origin, source, type, cause)
        }

        public fun fromString(string: String?): OriginNamespacedTag? {
            if (string.isNullOrBlank() || string.length < 13) return null
            tagCache.asMap().entries.firstOrNull { it.value.asString() == string }?.let { return it.key }

            return directGet(string)
        }

        public fun fromBukkitKey(key: NamespacedKey?): OriginNamespacedTag? {
            if (key == null) return null
            tagCache.asMap().entries.firstOrNull { it.value == key }?.let { return it.key }

            val tag = directGet(key.asString()) ?: return null
            tagCache.put(tag, key)
            return tag
        }

        internal fun baseStateOf(
            origin: OriginValues,
            state: State
        ): OriginNamespacedTag = OriginNamespacedTag(
            origin.name.lowercase(),
            Source.BASE,
            CauseType.STATE,
            state.name.lowercase()
        )

        internal fun baseFoodOf(
            origin: OriginValues,
            material: Material
        ): OriginNamespacedTag = OriginNamespacedTag(
            origin.name.lowercase(),
            Source.FOOD,
            CauseType.MATERIAL,
            material.name.lowercase()
        )

        public fun abilityCustomOf(
            origin: OriginValues,
            ability: Ability
        ): OriginNamespacedTag = OriginNamespacedTag(
            origin.name.lowercase(),
            Source.ABILITY,
            CauseType.CUSTOM,
            ability.name.lowercase()
        )

        @Deprecated("Use the other abilityCustomOf", ReplaceWith("abilityCustomOf(origin, ability)"))
        public fun abilityCustomOf(
            origin: OriginValues,
            ability: String
        ): OriginNamespacedTag = OriginNamespacedTag(
            origin.name.lowercase(),
            Source.ABILITY,
            CauseType.CUSTOM,
            ability.lowercase()
        )

        public fun AttributeModifierBuilder.applyTag(tag: OriginNamespacedTag): AttributeModifierBuilder {
            this.name = tag.asString
            return this
        }

        public fun PotionEffectBuilder.applyTag(tag: OriginNamespacedTag): PotionEffectBuilder {
            this.tag = tag
            return this
        }
    }
}
