package com.rarible.protocol.union.core.continuation.page

data class PageSize(
    val default: Int,
    val max: Int
) {

    companion object {
        // Taken from Ethereum-Indexer API
        val ITEM = PageSize(1000, 1000)
        val OWNERSHIP = PageSize(1000, 1000)
        val COLLECTION = PageSize(1000, 1000)
        val ORDER = PageSize(50, 1000)
        val AUCTION = PageSize(50, 1000)
        val ACTIVITY = PageSize(1000, 1000)
    }

    fun limit(size: Int?): Int {
        return Integer.min(size ?: default, max)
    }

}
