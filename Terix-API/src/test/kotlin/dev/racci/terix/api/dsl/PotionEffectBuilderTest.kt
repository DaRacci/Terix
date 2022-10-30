package dev.racci.terix.api.dsl

import Bootstrap
import dev.racci.minix.api.utils.ticks
import dev.racci.terix.api.TestOrigin
import dev.racci.terix.api.origins.abilities.Levitate
import dev.racci.terix.api.origins.states.State
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PotionEffectBuilderTest {

    private lateinit var angel: TestOrigin
    private lateinit var originKey: NamespacedKey
    private lateinit var originKeyO: NamespacedKey
    private lateinit var abilityKey: NamespacedKey
    private lateinit var abilityKeyO: NamespacedKey
    private lateinit var foodKey: NamespacedKey
    private lateinit var foodKeyO: NamespacedKey

    @BeforeAll
    fun setUp() {
        Bootstrap.startUp()
        angel = TestOrigin()
        originKey = NamespacedKey("terix", "origin_potion_testorigin/darkness")
        originKeyO = PotionEffectBuilder().originKey(angel, State.LightState.DARKNESS).key
        abilityKey = NamespacedKey("terix", "origin_ability_levitate")
        abilityKeyO = PotionEffectBuilder().abilityKey("temp", Levitate::class).key
        foodKey = NamespacedKey("terix", "origin_food_cod")
        foodKeyO = PotionEffectBuilder().foodKey(temp, Material.COD).key
    }

    @AfterAll
    fun tearDown() {
        Bootstrap.shutDown()
    }

    @Test
    fun `originKey returns correct key`() {
        assertEquals(originKey, originKeyO)
    }

    @Test
    fun `originKey matches regex`() {
        assertTrue(originKeyO.asString().matches(PotionEffectBuilder.regex))
    }

    @Test
    fun `abilityKey returns correct key`() {
        assertEquals(abilityKey, abilityKeyO)
    }

    @Test
    fun `abilityKey matches regex`() {
        assertTrue(abilityKeyO.asString().matches(PotionEffectBuilder.regex))
    }

    @Test
    fun `foodKey returns correct key`() {
        assertEquals(foodKey, foodKeyO)
    }

    @Test
    fun `foodKey matches regex`() {
        assertTrue(foodKeyO.asString().matches(PotionEffectBuilder.regex))
    }

    @Test
    fun `builder returns same as normal constructor`() {
        val builder = DSLMutator<PotionEffectBuilder> {
            type = PotionEffectType.LEVITATION
            duration = 35.ticks
            amplifier = 1
            ambient = true
            particles = false
            key = originKey
        }.asNew().get()
        val actual = PotionEffect(PotionEffectType.LEVITATION, 35, 1, true, false, originKey)

        assertEquals(actual, builder)
    }
}
