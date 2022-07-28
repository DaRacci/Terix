package dev.racci.terix.api.origins

import dev.racci.terix.api.Origin
import dev.racci.terix.api.origins.enums.Trigger
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.attribute.AttributeModifier
import org.bukkit.potion.PotionEffectType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import strikt.api.expectThat
import strikt.assertions.elementAt
import strikt.assertions.first
import strikt.assertions.hasSize
import strikt.assertions.isNotNull
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AbstractOriginTest {

    lateinit var origin: AbstractOrigin

    @BeforeAll
    fun setUp() {
        origin = Origin()
        runBlocking { origin.onRegister() }
    }

    @Test
    fun getName() {
        assertEquals("TestOrigin", origin.name)
    }

    @Test
    fun getColour() {
        assertEquals(NamedTextColor.AQUA, origin.colour)
    }

    @Test
    fun getDisplayName() {
        assertEquals(NamedTextColor.AQUA, origin.displayName.color())
        assertEquals("<aqua>TestOrigin", MiniMessage.miniMessage().serialize(origin.displayName))
    }

    @Test
    fun getHurtSound() {
        assertEquals("minecraft:entity.player.hurt", origin.sounds.hurtSound.resourceKey.asString())
    }

    @Test
    fun getDeathSound() {
        assertEquals("minecraft:entity.player.death", origin.sounds.deathSound.resourceKey.asString())
    }

    @Test
    fun getNightVision() {
        assertEquals(true, origin.nightVision)
    }

    @Test
    fun getWaterBreathing() {
        assertEquals(true, origin.waterBreathing)
    }

    @Test
    fun getFireImmune() {
        assertEquals(true, origin.fireImmune)
    }

    @Test
    fun getPermission() {
        assertEquals("test.permission", origin.permission)
    }

    @Test
    fun getBecomeOriginTitle() {
        assertEquals("test.permission", origin.permission)
    }

    @Test
    fun getAttributeModifiers() {
        assertEquals(2, origin.attributeModifiers.size)

        expectThat(origin.attributeModifiers[Trigger.DAY])
            .isNotNull().first()
            .assert("Attribute is attack damage", Attribute.GENERIC_ATTACK_DAMAGE) { it.first }
            .assert("Modifier value is 2.0", 2.0) { it.second.amount }
            .assert("Modifier operation is ADD_NUMBER", AttributeModifier.Operation.ADD_NUMBER) { it.second.operation }
            .assert("Modifier name is matches", "origin_modifier_origintest_day") { it.second.name }

        expectThat(origin.attributeModifiers[Trigger.ON])
            .isNotNull().elementAt(0)
            .assert("Modifier value is 0.5", 0.5) { it.second.amount }
            .assert("Modifier operation is MULTIPLY_SCALAR_1", AttributeModifier.Operation.MULTIPLY_SCALAR_1) { it.second.operation }

        expectThat(origin.attributeModifiers[Trigger.ON]!!)
            .elementAt(1)
            .assert("Modifier value is 0.25", 0.25) { it.second.amount }
            .assert("Modifier operation is MULTIPLY_SCALAR_1", AttributeModifier.Operation.MULTIPLY_SCALAR_1) { it.second.operation }
    }

    @Test
    fun getTitles() {
        expectThat(origin.titles)
            .hasSize(1)
            .get { this[Trigger.DAY] }
            .isNotNull()
            .assertThat("Title is correct") { MiniMessage.miniMessage().serialize(it.title!!) == "<green>Title" }
            .assertThat("Subtitle is correct") { MiniMessage.miniMessage().serialize(it.subtitle!!) == "<green>Subtitle" }
    }

    @Test
    fun getPotions() { // Note: If this fails it prints a really useless stack trace
        expectThat(origin.potions[Trigger.NETHER])
            .isNotNull()
            .first()
            .assertThat("Potion is correct") { it.type == PotionEffectType.REGENERATION }
            .assertThat("Potion duration is correct") { it.duration == 100 }
            .assertThat("Potion amplifier is correct") { 1 == it.amplifier }
            .assertThat("Potion ambient is correct") { it.isAmbient }
            .assertThat("Potion show particles is correct") { !it.hasParticles() }
            .assertThat("Potion icon is correct") { !it.hasIcon() }
            .assertThat("Potion key is correct") { "terix:origin_potion_testorigin/nether" == it.key.toString() }
        assertNotNull(origin.potions[Trigger.FLAMMABLE])
    }

    @Test
    fun getDamageTicks() {
        expectThat(origin.damageTicks[Trigger.WATER])
            .isNotNull()
            .assertThat("Damage ticks is correct") { it == 2.0 }
    }

    @Test
    fun `attribute modifiers apply correct amounts`() {
        val list = mutableListOf<AttributeModifier>()
        val inst = mockk<AttributeInstance> {
            every { value } answers {
                var d = baseValue

                for (attributeModifier in modifiers.filter { it.operation == AttributeModifier.Operation.ADD_NUMBER }) {
                    d += attributeModifier.amount
                }
                var e = d

                for (attributeModifier2 in modifiers.filter { it.operation == AttributeModifier.Operation.ADD_SCALAR }) {
                    e += d * attributeModifier2.amount
                }

                for (attributeModifier3 in modifiers.filter { it.operation == AttributeModifier.Operation.MULTIPLY_SCALAR_1 }) {
                    e *= 1.0 + attributeModifier3.amount
                }

                e
            }
            every { baseValue } returns 20.0
            every { modifiers } returns list
            every { addModifier(any()) } answers { list.add(firstArg()) }
        }

        val modi = origin.attributeModifiers[Trigger.ON]!!.first { it.first == Attribute.GENERIC_MAX_HEALTH }
        inst.addModifier(modi.second)

        expectThat(inst)
            .assertThat("Inst has 1 modifier") { it.modifiers.size == 1 }
            .assertThat("The new value is 20 * 0.5") { it.value == 10.0 }

        inst.addModifier(AttributeModifier("test2", 10.0, AttributeModifier.Operation.ADD_NUMBER))

        expectThat(inst)
            .assertThat("Inst has 2 modifiers") { it.modifiers.size == 2 }
            .assertThat("The new value is (20 + 10) * 0.5") { it.value == 15.0 }

        verify { inst.modifiers }
        verify { inst.value }
        verify { inst.baseValue }
        verify { inst.addModifier(any()) }
    }

    @Test
    fun getTriggerBlocks() {
    }

    @Test
    fun getDamageActions() {
    }

    @Test
    fun getFoodBlocks() {
    }

    @Test
    fun getFoodPotions() {
    }

    @Test
    fun getFoodAttributes() {
    }

    @Test
    fun getFoodMultipliers() {
    }

    @Test
    fun getAbilities() {
    }

    @Test
    fun getItem() {
    }

    @Test
    fun hasPermission() {
    }
}
