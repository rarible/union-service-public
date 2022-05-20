package com.rarible.protocol.union.core.model

data class EsItemSort(
    val latestFirst: Boolean= true,
) {
    companion object {
        val DEFAULT = EsItemSort()
    }
}
