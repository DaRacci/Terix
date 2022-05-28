package dev.racci.terix.core.extensions

import dev.racci.terix.api.OriginService
import dev.racci.terix.core.services.OriginServiceImpl
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.verify
import kotlinx.collections.immutable.persistentMapOf
import org.bukkit.NamespacedKey
import org.bukkit.potion.PotionEffect
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.dsl.binds
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.mock.MockProvider
import org.koin.test.mock.declareMock
import kotlin.test.assertTrue

internal class ExPotionKtTest : KoinTest {

    @get:ExtendWith
    val mockProvider = MockProvider.register { clazz ->
        mockkClass(clazz)
    }

    private val mockPotion = mockk<PotionEffect> {
        every { key } returns NamespacedKey.fromString("terix:origin_potion_angel/sunlight")
    }

    @Test
    fun fromOrigin() {
        assertTrue { mockPotion.fromOrigin() }
        verify { mockPotion.key }
    }

    @Test
    fun origin() {
        startKoin {
            loadKoinModules(
                module { single { mockk<OriginServiceImpl>() } binds arrayOf(OriginService::class, OriginServiceImpl::class) }
            )
        }
        declareMock<OriginService>()
        every { OriginServiceImpl.getService().registry } returns persistentMapOf("angel" to mockk())
        every { OriginService.getService().getOriginOrNull(any<String>()) } answers { callOriginal() }
    }
}
