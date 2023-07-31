package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.core.model.elastic.EsCollectionCursor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.time.Instant

internal class EsCollectionCursorTest {

    @TestFactory
    fun `should convert from`() = listOf(
        TestCase("", null),
        TestCase("_1000", null),
        TestCase("null_1000", null),
        TestCase("1665310765200_1000", EsCollectionCursor(Instant.ofEpochMilli(1665310765200), 1000)),
    ).map {
        dynamicTest("should convert from $it") {
            val continuation = EsCollectionCursor.fromString(it.str)

            if (it.result == null) {
                assertNull(continuation)
            } else {
                assertEquals(it.result, continuation)
            }
        }
    }
}

data class TestCase(val str: String, val result: EsCollectionCursor?)
