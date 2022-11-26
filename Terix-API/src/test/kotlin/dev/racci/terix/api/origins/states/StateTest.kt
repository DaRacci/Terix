package dev.racci.terix.api.origins.states

import Bootstrap
import dev.racci.minix.api.extensions.isNight
import io.kotest.core.spec.style.FunSpec
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.bukkit.World
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StateTest : FunSpec({

    beforeTest {
        Bootstrap.startUp()
    }

    afterEach {
        State.activeStates.clear()
    }

    test("Name") {
        assertEquals(State.CONSTANT.name, "CONSTANT")
    }

    test("Ordinal") {
        assertEquals(0, State.CONSTANT.ordinal)
        assertEquals(1, State.TimeState.DAY.ordinal)
        assertEquals(2, State.TimeState.NIGHT.ordinal)
        assertEquals(3, State.WorldState.OVERWORLD.ordinal)
        assertEquals(4, State.WorldState.NETHER.ordinal)
        assertEquals(5, State.WorldState.END.ordinal)
        assertEquals(6, State.LiquidState.WATER.ordinal)
        assertEquals(7, State.LiquidState.LAVA.ordinal)
        assertEquals(8, State.LiquidState.LAND.ordinal)
        assertEquals(9, State.LightState.SUNLIGHT.ordinal)
        assertEquals(10, State.LightState.DARKNESS.ordinal)
        assertEquals(11, State.WeatherState.RAIN.ordinal)
        assertEquals(12, State.WeatherState.SNOW.ordinal)
    }

    test("incompatible states test") {
        State.TimeState.DAY.activate(Bootstrap.mockPlayer, Bootstrap.mockOrigin)
        assertTrue("Player states should contain DAY.") { State.getPlayerStates(Bootstrap.mockPlayer).contains(State.TimeState.DAY) }

        State.TimeState.NIGHT.activate(Bootstrap.mockPlayer, Bootstrap.mockOrigin)
        assertFalse("Player states should not contain DAY.") { State.getPlayerStates(Bootstrap.mockPlayer).contains(State.TimeState.DAY) }
    }

    test("exchange states test") {
        State.WorldState.OVERWORLD.activate(Bootstrap.mockPlayer, Bootstrap.mockOrigin)
        State.WorldState.OVERWORLD.exchange(Bootstrap.mockPlayer, Bootstrap.mockOrigin, State.WorldState.NETHER)
        assertFalse("Player states shouldn't contain OVERWORLD.") { State.getPlayerStates(Bootstrap.mockPlayer).contains(State.WorldState.OVERWORLD) }
        assertTrue("Player states should contain NETHER.") { State.getPlayerStates(Bootstrap.mockPlayer).contains(State.WorldState.NETHER) }
    }

    fun playerAttributeSetup(): AttributeInstance {
        val mockkAttributeInstance = mockk<AttributeInstance> {
            every { attribute } returns Attribute.GENERIC_MAX_HEALTH
            every { addModifier(any()) } just Runs
            every { removeModifier(any()) } just Runs
        }
        every { Bootstrap.mockPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH) } returns mockkAttributeInstance

        return mockkAttributeInstance
    }

//    test("addAsync") {
//        val mockkAttributeInstance = playerAttributeSetup()
//
//        every { Bootstrap.mockOrigin.attributeModifiers[State.CONSTANT] } returns mutableListOf(Attribute.GENERIC_MAX_HEALTH to mockk())
//        State.CONSTANT.activate(Bootstrap.mockPlayer, Bootstrap.mockOrigin)
//
//        coVerify { mockkAttributeInstance.addModifier(any()) }
//    }

    test("getting time state") {
        val world = mockk<World> {
            every { environment } returns World.Environment.NETHER
            every { isDayTime } returns true
            every { isNight } returns true
        }
        assertNull(State.getTimeState(world))

        every { world.environment } returns World.Environment.NORMAL
        assertEquals(State.TimeState.DAY, State.getTimeState(world))

        every { world.isDayTime } returns false
        assertEquals(State.TimeState.NIGHT, State.getTimeState(world))
    }

//    test("getting weather state") {
//        val mockkBiome = mockk<Biome>() {
//            every { specialEffects } returns mockk()
//            every { precipitation } returns Biome.Precipitation.NONE
//            every { warmEnoughToRain(any()) } returns true
//            every { coldEnoughToSnow(any()) } returns true
//        }
//        val mockkWorld = mockk<CraftWorld>() {
//            every { hasStorm() } returns true
//            every { handle } returns mockk {
//                every { canSeeSky(any()) } returns true
//                every { getBiome(any()) } returns mockk { every { value() } returns mockkBiome }
//                every { getHeightmapPos(any(), any()) } returns mockk {
//                    every { y } returns 0
//                }
//            }
//        }
//        val mockkLocation = mockk<Location> {
//            every { block } returns mockk<CraftBlock>() {
//                every { position } returns mockk {
//                    every { y } returns 1
//                }
//            }
//            every { world } returns mockkWorld
//        }
//
//        assertNull(State.getWeatherState(mockkLocation))
//
//        every { mockkBiome.precipitation } returns Biome.Precipitation.RAIN
//        assertEquals(State.WeatherState.RAIN, State.getWeatherState(mockkLocation))
//
//        every { mockkBiome.precipitation } returns Biome.Precipitation.SNOW
//        assertEquals(State.WeatherState.SNOW, State.getWeatherState(mockkLocation))
//
//        every { mockkBiome.coldEnoughToSnow(any()) } returns false
//        assertNull(State.getWeatherState(mockkLocation))
//    }
})
