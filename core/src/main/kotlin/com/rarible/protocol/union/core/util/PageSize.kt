package com.rarible.protocol.union.core.util

import com.rarible.protocol.union.core.exception.UnionValidationException

data class PageSize(
    val default: Int,
    val max: Int
) {

    companion object {

        // Taken from Ethereum-Indexer API
        val ITEM = PageSize(50, 1000)
        val OWNERSHIP = PageSize(50, 1000)
        val COLLECTION = PageSize(50, 1000)
        val ORDER = PageSize(50, 1000)
        val AUCTION = PageSize(50, 1000)
        val ACTIVITY = PageSize(50, 1000)
    }

    fun limit(size: Int?): Int {
        if (size != null && size <= 0) {
            throw UnionValidationException("Page size should be a positive value limited by ${this.max}")
        }
        return Integer.min(size ?: default, max)
    }

}
