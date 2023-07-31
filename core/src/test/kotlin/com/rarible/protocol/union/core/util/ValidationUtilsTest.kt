package com.rarible.protocol.union.core.util

import com.rarible.protocol.union.core.exception.UnionValidationException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ValidationUtilsTest {

    @Test
    fun `check null ids - ok`() {
        checkNullIds(listOf<String>())
        checkNullIds(listOf("a"))
    }

    @Test
    fun `check null ids - null found`() {
        assertThrows(UnionValidationException::class.java) { checkNullIds(listOf<String?>(null)) }
        assertThrows(UnionValidationException::class.java) { checkNullIds(listOf("a", null)) }
    }
}
