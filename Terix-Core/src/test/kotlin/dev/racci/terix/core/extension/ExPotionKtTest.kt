package dev.racci.terix.core.extension

import org.bukkit.NamespacedKey
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.koin.core.KoinApplication
import org.koin.core.context.GlobalContext.startKoin
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ExPotionKtTest {

    @BeforeAll
    fun setUp() {
        startKoin(KoinApplication.init())
    }

    @Test
    fun fromOrigin() {
        val potion = PotionEffect(PotionEffectType.ABSORPTION, 20, 1, true, false, null)
        assertFalse(potion.fromOrigin())
        assertFalse(potion.withKey(NamespacedKey("test", "test")).fromOrigin())
        assertTrue(potion.withKey(NamespacedKey("origin", "test")).fromOrigin())
    }
}
