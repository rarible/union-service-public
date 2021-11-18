package com.rarible.protocol.union.core.converter

import com.rarible.protocol.union.core.exception.UnionValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.math.BigInteger

class UnionConverterTest {

    @Test
    fun `to BigInteger`() {
        assertThat(UnionConverter.convertToBigInteger("123")).isEqualTo(BigInteger.valueOf(123))
        assertThrows<UnionValidationException> {
            UnionConverter.convertToBigInteger("abc")
        }
    }

    @Test
    fun `to BigDecimal`() {
        assertThat(UnionConverter.convertToBigDecimal("123.3")).isEqualTo(BigDecimal.valueOf(123.3))
        assertThrows<UnionValidationException> {
            UnionConverter.convertToBigDecimal("abc")
        }
    }

    @Test
    fun `to Long`() {
        assertThat(UnionConverter.convertToLong("123")).isEqualTo(123L)
        assertThrows<UnionValidationException> {
            UnionConverter.convertToLong("abc")
        }
    }

}