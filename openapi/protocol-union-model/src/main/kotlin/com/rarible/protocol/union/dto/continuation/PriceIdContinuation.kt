package com.rarible.protocol.union.dto.continuation

import java.math.BigDecimal

data class PriceIdContinuation(
    val price: BigDecimal?,
    val id: String,
    val asc: Boolean = false
) : Continuation<PriceIdContinuation> {

    private val sign = if (asc) 1 else -1
    private val safePrice = price ?: BigDecimal.ZERO

    override fun compareTo(other: PriceIdContinuation): Int {
        val priceDiff = this.safePrice.compareTo(other.safePrice)
        if (priceDiff != 0) {
            return sign * priceDiff
        }
        return sign * this.id.compareTo(other.id)
    }

    override fun toString(): String {
        return "${safePrice}_${id}"
    }

    companion object {
        fun parse(str: String?): PriceIdContinuation? {
            val pair = Continuation.splitBy(str, "_") ?: return null
            val timestamp = pair.first
            val id = pair.second
            return PriceIdContinuation(BigDecimal(timestamp), id)
        }
    }
}