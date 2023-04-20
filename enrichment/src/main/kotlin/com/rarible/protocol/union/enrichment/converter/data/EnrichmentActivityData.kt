package com.rarible.protocol.union.enrichment.converter.data

import com.rarible.protocol.union.dto.CollectionIdDto

data class EnrichmentActivityData(
    val customCollection: CollectionIdDto? = null
) {

    companion object {

        private val EMPTY = EnrichmentActivityData()
        fun empty() = EMPTY
    }

}