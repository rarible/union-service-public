package com.rarible.protocol.union.api.service.elastic

import com.rarible.protocol.union.core.model.ElasticItemFilter
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ItemFilterConverter(
) {

    fun convertGetAllItems(
        blockchains: Set<String>,
        showDeleted: Boolean?,
        lastUpdatedFrom: Long?,
        lastUpdatedTo: Long?,
        cursor: String?
    ): ElasticItemFilter = ElasticItemFilter(
        cursor = cursor,
        blockchains = blockchains,
        deleted = showDeleted,
        updatedFrom = lastUpdatedFrom?.let { Instant.ofEpochMilli(it) },
        updatedTo = lastUpdatedTo?.let { Instant.ofEpochMilli(it) },
    )
}
