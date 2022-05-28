package dev.racci.terix.services

import dev.racci.minix.api.scheduler.CoroutineScheduler
import dev.racci.minix.api.utils.unsafeCast
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.core.extensions.inSunlight
import dev.racci.terix.core.extensions.wasInSunlight
import dev.racci.terix.core.services.RunnableService
import dev.racci.terix.services.Bootstrap.mockOrigin
import dev.racci.terix.services.Bootstrap.mockPlayer
import dev.racci.terix.services.Bootstrap.mockPlugin
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.spyk
import io.mockk.verify
import kotlinx.collections.immutable.PersistentMap
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.junit5.KoinTestExtension
import org.koin.test.junit5.mock.MockProviderExtension
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.first
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertEquals

internal class RunnableServiceTest : KoinTest {

    private lateinit var instance: RunnableService

    @JvmField
    @RegisterExtension
    val koinTestRule = KoinTestExtension.create {
        modules(
            listOf(
                module { single { mockPlugin } },
                module { single { mockk<CoroutineScheduler>() } }
            )
        )
    }

    @JvmField
    @RegisterExtension
    val mockProvider = MockProviderExtension.create { clazz ->
        mockkClass(clazz)
    }

    private val functions get() = RunnableService::class.declaredMemberProperties.first {
        it.name == "functions"
    }.also { it.isAccessible = true }

    @BeforeEach
    fun setUp() {
        Bootstrap.startUp()
        instance = spyk(RunnableService(mockPlugin), recordPrivateCalls = true) {
            coEvery { doInvoke(any(), any(), any(), any(), any()) } just Runs
            every { this@spyk getProperty "functions" } answers { callOriginal() }
            every { getTasks(any()) } answers { callOriginal() }
        }
    }

    @AfterEach
    fun tearDown() {
        Bootstrap.shutDown()
    }

    @Test
    fun `all functions are present`() {
        expectThat(functions.call(instance).unsafeCast<PersistentMap<Trigger, *>>()) {
            hasSize(4)
            get { this[Trigger.SUNLIGHT] }.isEqualTo(RunnableService::doSunlightTick)
            get { this[Trigger.DARKNESS] }.isEqualTo(RunnableService::doDarknessTick)
            get { this[Trigger.RAIN] }.isEqualTo(RunnableService::doRainTick)
            get { this[Trigger.WATER] }.isEqualTo(RunnableService::doWaterTick)
        }
    }

    @Test
    fun `player receives no tasks`() {
        assertEquals(0, instance.getTasks(mockOrigin).size)
    }

    @Test
    fun `player receives 1 task`() {
        every { mockOrigin.titles } returns mutableMapOf(Pair(Trigger.SUNLIGHT, mockk()))
        every { mockOrigin.damageTicks } returns mutableMapOf(Pair(Trigger.SUNLIGHT, mockk()))

        expectThat(instance.getTasks(mockOrigin)) {
            hasSize(1)
            first().isEqualTo(RunnableService::doSunlightTick)
        }

        verify { mockOrigin.titles }
        verify { mockOrigin.damageTicks }
    }

    @Test
    fun `player receives water and rain task`() {
        every { mockOrigin.titles } returns mutableMapOf(Pair(Trigger.WET, mockk()))

        expectThat(instance.getTasks(mockOrigin)) {
            hasSize(2)
            containsExactlyInAnyOrder(RunnableService::doWaterTick, RunnableService::doRainTick)
        }

        verify { mockOrigin.titles }
    }

    private fun genericSunlightSetup() {
        coEvery { instance.shouldTickSunlight(any()) } returns true
        every { mockOrigin.damageTicks[Trigger.SUNLIGHT] } returns 10.0
        every { mockPlayer.wasInSunlight } returns false
        every { mockPlayer.inSunlight } returns true
        every { mockPlayer.inventory } returns mockk {
            every { helmet } returns null
        }
        every { mockPlayer.fireTicks } returns 0
        every { mockPlayer.fireTicks = any() } just Runs
    }

    @Test
    fun `sunlight tick applies to player`() {
        genericSunlightSetup()
        runBlocking { instance.doSunlightTick(mockPlayer, mockOrigin) }

        verify { mockOrigin.damageTicks[Trigger.SUNLIGHT] }
        verify { mockPlayer.wasInSunlight }
        verify { mockPlayer.inSunlight }
        verify { mockPlayer.inventory }
        verify { mockPlayer.fireTicks }
    }

/*     @Test
    fun `sunlight task tests`() {
        genericSunlightSetup()
        every { mockPlayer.fireTicks } returns 11
        runBlocking { instance.doSunlightTick(mockPlayer, mockOrigin) }

        genericSunlightSetup()
        loadKoinModules(
            module {
                single {
                    mockk<HookService> {
                        every { get(kClass = any<KClass<HookService.HookService>>()) } returns null
                    }
                }
            }
        )
        declareMock<HookService>()
        every { mockPlayer.inventory.helmet } returns mockk {
            every { enchantments } returns mutableMapOf()
            every { hasEnchant(any()) } answers { callOriginal() }
        }

        every { mockPlayer.toNMS().random.nextInt(any(), any()) } returns 2
        every { mockPlayer.inventory.helmet } returns mockk<CraftItemStack>() {
            every { damage } returns 0
            every { maxItemUseDuration } returns 10
            every { handle } returns mockk()
        }
        every { (mockPlayer.inventory.helmet as CraftItemStack).handle } returns mockk {
            every { hurtAndBreak(any(), any(), any()) } just Runs
            every { hurt(any(), any(), any()) } returns true
        }

        runBlocking { instance.doSunlightTick(mockPlayer, mockOrigin) }
        verifyAll {
            mockPlayer.inventory.helmet!!.damage
            mockPlayer.inventory.helmet!!.maxItemUseDuration
            (mockPlayer.inventory.helmet!! as CraftItemStack).handle
            (mockPlayer.inventory.helmet!! as CraftItemStack).handle.hurtAndBreak(any(), any(), any()) wasNot Called
            (mockPlayer.inventory.helmet!! as CraftItemStack).handle.hurt(any(), any(), any())
        }

        every { mockPlayer.inventory.helmet!!.damage } returns 9
        runBlocking { instance.doSunlightTick(mockPlayer, mockOrigin) }
        verify { (mockPlayer.inventory.helmet!! as CraftItemStack).handle.hurtAndBreak(any(), any(), any()) }
    } */
}
