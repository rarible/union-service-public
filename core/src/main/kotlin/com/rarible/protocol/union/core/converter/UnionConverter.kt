package com.rarible.protocol.union.core.converter

import com.rarible.protocol.union.core.exception.UnionValidationException
import java.math.BigDecimal
import java.math.BigInteger

object UnionConverter {

    fun convertToBigInteger(value: String): BigInteger {
        try {
            return BigInteger(value)
        } catch (e: Throwable) {
            throw UnionValidationException("Incorrect BigInteger format: $value")
        }
    }

    fun convertToLong(value: String): Long {
        try {
            return value.toLong()
        } catch (e: Throwable) {
            throw UnionValidationException("Incorrect Long format: $value")
        }
    }

    fun convertToBigDecimal(value: String): BigDecimal {
        try {
            return BigDecimal(value)
        } catch (e: Throwable) {
            throw UnionValidationException("Incorrect BigDecimal format: $value")
        }
    }
}
