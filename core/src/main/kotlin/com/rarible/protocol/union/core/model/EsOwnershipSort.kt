package com.rarible.protocol.union.core.model

enum class EsOwnershipSort {
    LATEST_FIRST,
    EARLIEST_FIRST,
    HIGHEST_SELL_PRICE_FIRST,
    LOWEST_SELL_PRICE_FIRST;

    companion object {
        val DEFAULT = LATEST_FIRST
    }
}
