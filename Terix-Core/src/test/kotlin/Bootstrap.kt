import com.mojang.datafixers.DataFixerBuilder
import dev.racci.minix.api.scheduler.CoroutineRunnable
import dev.racci.minix.api.scheduler.CoroutineScheduler
import dev.racci.minix.api.scheduler.CoroutineTask
import dev.racci.terix.api.Terix
import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.origins.origin.Origin
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
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer
import org.bukkit.entity.Player
import java.util.UUID
import java.util.function.Supplier

object Bootstrap {

    @MockK
    lateinit var mockPlugin: Terix

    @MockK
    lateinit var mockTask: CoroutineTask

    @MockK
    lateinit var mockPlayer: Player

    @MockK
    lateinit var mockOrigin: Origin

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
                every { trace(any(), any(), any<() -> Any?>()) } just Runs
                every { debug(any(), any(), any<() -> Any?>()) } just Runs
                every { info(allAny()) } just Runs
                every { warn(allAny()) } just Runs
                every { error(allAny()) } just Runs
            }
        }

//        every { mockOrigin.stateTitles } returns mutableMapOf()
//        every { mockOrigin.statePotions } returns multiMapOf()
//        every { mockOrigin.stateDamageTicks } returns mutableMapOf()
//        every { mockOrigin.stateBlocks } returns mutableMapOf()
//        every { mockOrigin.attributeModifiers } returns multiMapOf()

        every { mockPlayer.name } returns "TestPlayer"
        every { mockPlayer.uniqueId } returns UUID.randomUUID()

        every { mockTask.taskID } returns 1
        every { mockTask.name = any() } just Runs

        mockkStatic("dev.racci.terix.core.extensions.ExPlayerKt")
        mockkObject(CoroutineScheduler)
        every { TerixPlayer.cachedOrigin(mockPlayer) } returns mockOrigin

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
