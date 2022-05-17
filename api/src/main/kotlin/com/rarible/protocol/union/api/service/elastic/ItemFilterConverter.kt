package com.rarible.protocol.union.api.service.elastic

import com.rarible.protocol.union.core.model.ElasticItemFilter
import com.rarible.protocol.union.core.model.EsItemCursor
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.continuation.CombinedContinuation
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
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
    ): ElasticItemFilter {

        val esItemCursor = if (cursor != null) {
            val currentContinuation = CombinedContinuation.parse(cursor)
            val entry = currentContinuation.continuations.entries.first()
            val dateIdContinuation = DateIdContinuation.parse(entry.value)
            EsItemCursor(
                date = dateIdContinuation!!.date,
                itemId = ItemIdDto(BlockchainDto.valueOf(entry.key), dateIdContinuation!!.id).toString()
            )
        } else null

        return ElasticItemFilter(
            cursor = esItemCursor,
            blockchains = blockchains,
            deleted = showDeleted,
            updatedFrom = lastUpdatedFrom?.let { Instant.ofEpochMilli(it) },
            updatedTo = lastUpdatedTo?.let { Instant.ofEpochMilli(it) },
        )
    }
}
