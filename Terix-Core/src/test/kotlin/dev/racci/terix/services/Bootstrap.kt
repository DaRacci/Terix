package dev.racci.terix.services

import com.mojang.datafixers.DataFixerBuilder
import dev.racci.minix.api.scheduler.CoroutineRunnable
import dev.racci.minix.api.scheduler.CoroutineScheduler
import dev.racci.minix.api.scheduler.CoroutineTask
import dev.racci.minix.api.utils.collections.multiMapOf
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.core.extensions.origin
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import net.minecraft.SharedConstants
import net.minecraft.world.level.storage.DataVersion
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer
import org.bukkit.entity.Player
import java.util.UUID
import java.util.function.Supplier

internal object Bootstrap {

    @MockK
    lateinit var mockPlugin: Terix

    @MockK
    lateinit var mockTask: CoroutineTask

    @MockK
    lateinit var mockPlayer: Player

    @MockK
    lateinit var mockOrigin: AbstractOrigin

    fun startUp() {
        MockKAnnotations.init(this, relaxed = true, relaxUnitFun = true)

        mockkStatic(net.minecraft.server.Bootstrap::class)
        mockkStatic(SharedConstants::class)
        every { net.minecraft.server.Bootstrap.checkBootstrapCalled(any<Supplier<String>>()) } returns Unit
        every { SharedConstants.getCurrentVersion() } returns mockk {
            every { worldVersion } returns 2975
            every { seriesId } returns "main"
            every { dataVersion } returns mockk {
                every { id } returns "2975"
                every { series } returns DataVersion.MAIN_SERIES
            }
        }
        mockkConstructor(DataFixerBuilder::class)
        every { anyConstructed<DataFixerBuilder>().addFixer(any()) } just Runs
        mockPlayer = mockk<CraftPlayer>()

        every { mockPlugin.log } answers {
            mockk {
                every { debug(any<Throwable>(), allAny()) } just Runs
                every { info(any<Throwable>(), allAny()) } just Runs
                every { warn(any<Throwable>(), allAny()) } just Runs
                every { error(any<Throwable>(), allAny()) } just Runs
                every { trace(any<Throwable>(), allAny()) } just Runs
            }
        }

        every { mockOrigin.titles } returns mutableMapOf()
        every { mockOrigin.potions } returns multiMapOf()
        every { mockOrigin.damageTicks } returns mutableMapOf()
        every { mockOrigin.triggerBlocks } returns mutableMapOf()
        every { mockOrigin.attributeModifiers } returns multiMapOf()

        every { mockPlayer.name } returns "TestPlayer"
        every { mockPlayer.uniqueId } returns UUID.randomUUID()

        every { mockTask.taskID } returns 1
        every { mockTask.name = any() } just Runs

        mockkStatic("dev.racci.terix.core.extensions.ExPlayerKt")
        mockkObject(CoroutineScheduler)
        every { mockPlayer.origin() } returns mockOrigin

        every { CoroutineScheduler.runAsyncTaskTimer(allAny(), allAny<CoroutineRunnable>(), allAny(), allAny()) } answers {
            mockk {
                every { taskID } returns 1
                every { name = any() } just Runs
            }
        }
    }

    fun shutDown() {
//        clearAllMocks()
//        stopKoin()
    }
}
