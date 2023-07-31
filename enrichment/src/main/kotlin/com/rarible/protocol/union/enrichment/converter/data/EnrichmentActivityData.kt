package com.rarible.protocol.union.enrichment.converter.data

import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.CollectionIdDto

data class EnrichmentActivityData(
    val customCollections: Map<ActivityIdDto, CollectionIdDto> = emptyMap()
) {

    companion object {

        private val EMPTY = EnrichmentActivityData()
        fun empty() = EMPTY
    }
}
