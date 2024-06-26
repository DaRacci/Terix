import com.mojang.datafixers.DataFixerBuilder
import dev.racci.minix.api.scheduler.CoroutineRunnable
import dev.racci.minix.api.scheduler.CoroutineScheduler
import dev.racci.minix.api.scheduler.CoroutineTask
import dev.racci.minix.api.utils.KoinUtils
import dev.racci.minix.core.services.PluginServiceImpl
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.Terix
import dev.racci.terix.api.data.player.TerixPlayer
import dev.racci.terix.api.data.player.TerixUser
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.sentryScoped
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.minecraft.SharedConstants
import net.minecraft.world.level.storage.DataVersion
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module
import java.util.UUID
import java.util.function.Supplier

object Bootstrap {

    @MockK
    lateinit var mockPlugin: Terix

    @MockK
    lateinit var mockTask: CoroutineTask

    @MockK
    lateinit var mockkScheduler: CoroutineScheduler

    @MockK
    lateinit var mockPlayer: TerixPlayer

    @MockK
    lateinit var mockOrigin: Origin

    @MockK
    lateinit var mockOriginService: OriginService

    private var started = false

    fun startUp() {
        if (started) return
        started = true

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
        mockPlayer = mockk()

        startKoin()
        mockkPlugin()

//        every { mockOrigin.stateTitles } returns mutableMapOf()
//        every { mockOrigin.statePotions } returns multiMapOf()
//        every { mockOrigin.stateDamageTicks } returns mutableMapOf()
//        every { mockOrigin.stateBlocks } returns mutableMapOf()
//        every { mockOrigin.attributeModifiers } returns multiMapOf()

        every { mockPlayer.name } returns "TestPlayer"
        every { mockPlayer.uniqueId } returns UUID.randomUUID()
        every { mockPlayer.gameMode } returns GameMode.SURVIVAL

        every { mockTask.taskID } returns 1
        every { mockTask.name = any() } just Runs

        mockkStatic("dev.racci.terix.core.extensions.ExPlayerKt")
        mockkStatic(::sentryScoped)

        coEvery { sentryScoped(any(), any(), any(), any(), any(), any(), any()) } coAnswers {
            val block = it.invocation.args.filterIsInstance<suspend () -> Unit>().first()
            block()
        }

        every { mockkScheduler.runAsyncTaskTimer(allAny(), allAny<CoroutineRunnable>(), allAny(), allAny()) } answers {
            mockk {
                every { taskID } returns 1
                every { name = any() } just Runs
            }
        }

        mockkObject(TerixPlayer.Companion)
        mockkObject(TerixUser)

        every { TerixUser.origin } returns mockk()
        every { TerixUser.lastChosenTime } returns mockk()
        every { TerixUser.freeChanges } returns mockk()

        every { TerixPlayer[mockPlayer].origin } returns mockOrigin
        every { TerixPlayer[allAny<Player>()].origin } returns mockOrigin
    }

    private fun startKoin(): KoinApplication {
        return startKoin {
            modules(
                module {
                    single { mockPlugin } bind Terix::class
                    single { mockOriginService } bind OriginService::class
                    single { mockkScheduler } bind CoroutineScheduler::class
                    val pluginServiceMock = mockk<PluginServiceImpl>() {
                        every { coroutineSession[mockPlugin] } returns mockk {
                            every { minecraftDispatcher } returns Dispatchers.Unconfined
                            every { asyncDispatcher } returns Dispatchers.Unconfined
                            every { launch(any(), any(), any(), any()) } answers {
                                val block = it.invocation.args.filterIsInstance<suspend CoroutineScope.() -> Unit>().first()
                                runBlocking(block = block)
                                mockk()
                            }
                        }
                    }
                    single {
                        pluginServiceMock
                    } binds KoinUtils.getBinds(pluginServiceMock)
                }
            )
        }
    }

    private fun mockkOriginService() {
    }

    private fun mockkPlugin() {
        every { mockPlugin.log } answers {
            mockk {
                every { trace(any(), any(), any<() -> Any?>()) } just Runs
                every { debug(any(), any(), any<() -> Any?>()) } just Runs
                every { info(allAny()) } just Runs
                every { warn(allAny()) } just Runs
                every { error(allAny()) } just Runs
            }
        }
//
//        every { mockPlugin.launch(any(), any(), any()) } coAnswers {
//            val block = it.invocation.args[3] as suspend CoroutineScope.() -> Unit
//            block.invoke(mockk())
//            mockk()
//        }

//        mockkStatic(MinixPlugin::launch)
//        every { mockPlugin.launch(any(), any(), any()) } coAnswers {
//            val block = it.invocation.args[1] as suspend CoroutineScope.() -> Unit
//            coroutineScope(block)
//            mockk()
//        }
    }

    fun shutDown() {
//        clearAllMocks()
//        stopKoin()
    }
}
