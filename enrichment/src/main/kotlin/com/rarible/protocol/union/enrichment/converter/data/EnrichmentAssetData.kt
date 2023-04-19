package com.rarible.protocol.union.enrichment.converter.data

import com.rarible.protocol.union.dto.CollectionIdDto

data class EnrichmentAssetData(
    val customCollection: CollectionIdDto? = null
) {

    companion object {

        private val EMPTY = EnrichmentAssetData()
        fun empty() = EMPTY
    }
}