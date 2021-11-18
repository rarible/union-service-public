package com.rarible.protocol.union.core.continuation

import java.math.BigDecimal

data class UsdPriceIdContinuation(
    private val currencyId: String,
    private val price: BigDecimal?,
    private val priceUsd: BigDecimal?,
    private val id: String,
    private val asc: Boolean = false
) : Continuation<UsdPriceIdContinuation> {

    private val sign = if (asc) 1 else -1

    // USD used only for sorting, currency price used for continuation and secondary sorting
    override fun compareTo(other: UsdPriceIdContinuation): Int {
        // Firstly we're using sorting by USD
        var result = compareNullable(this.priceUsd, other.priceUsd)
        if (result != 0) return result

        // If we can't convert currency to USD, then sort by currencyId
        result = currencyId.compareTo(other.currencyId)
        if (result != 0) return result

        // Then we compare prices inside same currency
        result = compareNullable(this.price, other.price)
        if (result != 0) return result

        // Otherwise - using sorting by OrderId
        return this.id.compareTo(other.id) * sign
    }

    private fun compareNullable(thisPrice: BigDecimal?, otherPrice: BigDecimal?): Int {
        return if (thisPrice == null) {
            if (otherPrice == null) 0 else 1
        } else {
            if (otherPrice == null) -1 else thisPrice.compareTo(otherPrice) * sign
        }
    }

    override fun toString(): String {
        // In case if we have null prices (originally, we should not),
        // continuation should ignore price by using min/max value depends on sort
        val from = price ?: if (asc) "0" else Long.MAX_VALUE.toString()
        return "${from}_${id}"
    }

}