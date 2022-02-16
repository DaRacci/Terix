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
}
