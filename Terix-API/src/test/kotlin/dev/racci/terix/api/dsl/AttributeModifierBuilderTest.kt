package dev.racci.terix.api.dsl

import Bootstrap
import dev.racci.terix.api.TestOrigin
import dev.racci.terix.api.origins.states.State
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AttributeModifierBuilderTest {

    private lateinit var angel: TestOrigin
    private lateinit var name: String
    private lateinit var originName: String

    @BeforeAll
    fun setUp() {
        Bootstrap.startUp()
        angel = TestOrigin()
        name = "origin_modifier_testorigin_darkness"
        originName = AttributeModifierBuilder.originName(angel, State.LightState.DARKNESS)
    }

    @AfterAll
    fun tearDown() {
        Bootstrap.shutDown()
    }

    @Test
    fun `originName method returns correct string`() {
        assertEquals(originName, name)
    }

    @Test
    fun `originName matches correct regex`() {
        assertTrue(originName.matches(AttributeModifierBuilder.regex))
    }

    @Test
    fun `builder returns the same as normal constructor`() {
        val uuid = UUID.randomUUID()
        val builder = AttributeModifierBuilder {
            attribute = Attribute.GENERIC_MAX_HEALTH
            operation = AttributeModifier.Operation.ADD_NUMBER
            amount = 7.3
            name = originName
            this.uuid = uuid
        }.build()
        val actual = AttributeModifier(uuid, originName, 7.3, AttributeModifier.Operation.ADD_NUMBER)

        assertEquals(actual, builder)
    }
}
