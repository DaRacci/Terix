package dev.racci.terix.core.extension

import org.junit.jupiter.api.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class ExCollectionKtTest {

    @Test
    fun getCast() {
        val collection: Array<Any?> = arrayOf(1, "2", 3.0)
        assertTrue(collection.getCast<Int>(0) is Int)
        assertTrue(collection.getCast<String>(1) is String)
        assertTrue(collection.getCast<Double>(2) is Double)
        assertNull(collection.getCast<List<String>>(3))
    }

    @Test
    fun test() {
        val originCount = 45
        val origins = mutableListOf<Int>()
        repeat(originCount) {
            origins += 1
        }
        val grid = mutableListOf<MutableMap<Pair<Int, Int>, Int>>()
        repeat(10) {
            grid += mutableMapOf()
        }
        var x = 0
        var y = 0
        var i = 0
        var e = 0
        var m = 6
        var p = 0
        while (i < originCount) {
            val cord = x + ((e - y).takeIf { it > 0 } ?: 0)
            grid[p][cord to y] = origins[i]
            i++
            when {
                e > 5 -> { x = 0; y = 0; e = 0; m = 6; p++ }
                x < (6 - e) -> { x++ }
                else -> { x = 0; y++; e += 2; m -= 2 }
            }
        }

        for (p in grid) {
            for (y in 0..(p.keys.maxByOrNull { it.second }?.second ?: continue)) {
                for (x in 0..(p.keys.maxByOrNull { it.first }?.first ?: continue)) {
                    print("${p[x to y] ?: " "} ")
                }
                println()
            }
        }
    }
}

private operator fun <E> List<E>.component6(): E = this[5]
