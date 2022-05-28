package dev.racci.terix.core.origins

import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.services.Bootstrap.mockOrigin
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
        every { mockOrigin.titles[Trigger.SUNLIGHT] } returns mockk {
            every { this@mockk.invoke(any<Player>()) } just Runs
        }
    }

    private fun mockTriggerBlocks() {
        every { mockOrigin.triggerBlocks[Trigger.SUNLIGHT] } returns mockk {
            coEvery { this@mockk.invoke(any<Player>()) } just Runs
        }
    }

    private fun mockAttributeModifiers() {
        every { mockOrigin.attributeModifiers[Trigger.SUNLIGHT] } returns mockk()
    }

    private fun mockPotions() {
        every { mockOrigin.potions[Trigger.SUNLIGHT] } returns mutableListOf(
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
