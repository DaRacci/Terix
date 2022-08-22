package dev.racci.terix.core.origins

import Bootstrap.mockOrigin
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.origins.states.State
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType

internal class TriggerInvokerKtTest {

    /* @BeforeEach
    fun setUp() {
        Bootstrap.startUp()

        every { mockPlayer.addAttributeModifiers(any()) } answers { callOriginal() }
        every { mockPlayer.removeAttributeModifiers(any()) } answers { callOriginal() }
    }

    @AfterEach
    fun tearDown() {
        Bootstrap.shutDown()
    } */

    private fun mockTitles() {
        every { mockOrigin.titles[State.LightState.SUNLIGHT] } returns mockk {
            every { this@mockk.invoke(any<Player>()) } just Runs
        }
    }

    private fun mockTriggerBlocks() {
        every { mockOrigin.triggerBlocks[State.LightState.SUNLIGHT] } returns mockk {
            coEvery { this@mockk.invoke(any<Player>()) } just Runs
        }
    }

    private fun mockAttributeModifiers() {
        every { mockOrigin.attributeModifiers[State.LightState.SUNLIGHT] } returns mockk()
    }

    private fun mockPotions() {
        every { mockOrigin.potions[State.LightState.SUNLIGHT] } returns mutableListOf(
            PotionEffectBuilder.build {
                type = PotionEffectType.REGENERATION
                originKey("origin", "sunlight")
            },
            PotionEffectBuilder.build {
                type = PotionEffectType.SPEED
                originKey("origin", "sunlight")
            }
        )
    }

//    @Test
//    fun `test apply asyncable`() {
//        runBlocking { applyAsyncable(mockPlayer, mockOrigin, Trigger.SUNLIGHT, false) }
//    }
}
