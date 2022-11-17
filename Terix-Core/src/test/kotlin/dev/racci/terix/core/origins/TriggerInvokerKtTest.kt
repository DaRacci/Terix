package dev.racci.terix.core.origins

import Bootstrap.mockOrigin
import dev.racci.terix.api.origins.states.State
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.bukkit.entity.Player

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
        every { mockOrigin.stateTitles[State.LightState.SUNLIGHT] } returns mockk {
            every { this@mockk.invoke(any<Player>()) } just Runs
        }
    }

    private fun mockTriggerBlocks() {
        every { mockOrigin.stateBlocks[State.LightState.SUNLIGHT] } returns mockk {
            coEvery { this@mockk.invoke(any<Player>()) } just Runs
        }
    }

    private fun mockAttributeModifiers() {
        every { mockOrigin.attributeModifiers[State.LightState.SUNLIGHT] } returns mockk()
    }

//    @Test
//    fun `test apply asyncable`() {
//        runBlocking { applyAsyncable(mockPlayer, mockOrigin, Trigger.SUNLIGHT, false) }
//    }
}
