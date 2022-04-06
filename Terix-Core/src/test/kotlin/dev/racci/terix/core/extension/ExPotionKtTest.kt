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

    @Test
    fun test() {
        val list = mutableListOf<Int>()
        for (i in 0..27) {
            list.add(i)
        }

        val l1 = mutableListOf<Any>()
        val l2 = mutableListOf<Any>()
        val l3 = mutableListOf<Any>()
        val l4 = mutableListOf<Any>()
        val l5 = mutableListOf<Any>()
        val l6 = mutableListOf<Any>()

        repeat(9) {
            l1.add("-")
            l2.add("-")
            l3.add("-")
            l4.add("-")
            l5.add("-")
            l6.add("-")
        }

        var x = 0
        var y = 0
        list.forEachIndexed { index, i ->
            println("$index: $i")
            println("x: $x")
            println("y: $y")

            when (y) {
                0 -> l1[x] = i
                1 -> l2[x] = i
                2 -> l3[x] = i
                3 -> l4[x] = i
                4 -> l5[x] = i
                5 -> l6[x] = i
            }

            if (x / 7 < 1) {
                x++
            } else {
                x = 0
                y++
            }
        }

        println(l1)
        println(l2)
        println(l3)
        println(l4)
        println(l5)
        println(l6)
    }
}
