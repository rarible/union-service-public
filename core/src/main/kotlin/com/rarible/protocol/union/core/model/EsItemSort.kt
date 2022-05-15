package com.rarible.protocol.union.core.model

data class EsItemSort(
    val latestFirst: Boolean? = null,
    val byId: Boolean? = null,
    val mintedFirst: Boolean? = null,
    val scored: Boolean = false,
) {
    companion object {
        val DEFAULT = EsItemSort(byId = true)
    }
}
