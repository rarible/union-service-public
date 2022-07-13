package com.rarible.protocol.union.api.service.elastic

import com.rarible.protocol.union.core.model.EsItemCursor
import com.rarible.protocol.union.core.model.EsItemFilter
import com.rarible.protocol.union.core.model.EsItemGenericFilter
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
    ): EsItemFilter {
        return EsItemGenericFilter(
            cursor = cursor,
            blockchains = blockchains,
            deleted = showDeleted,
            updatedFrom = lastUpdatedFrom?.let { Instant.ofEpochMilli(it) },
            updatedTo = lastUpdatedTo?.let { Instant.ofEpochMilli(it) },
        )
    }

    fun getItemsByCollection(collectionId: String, cursor: String?): EsItemFilter {

        return EsItemGenericFilter(
            cursor = cursor,
            collections = setOf(collectionId)
        )
    }

    fun getItemsByOwner(owner: String, blockchains: Set<String>?, cursor: String?): EsItemFilter {

        return EsItemGenericFilter(
            cursor = cursor,
            owners = setOf(owner),
            blockchains = blockchains
        )
    }

    fun getItemsByCreator(creator: String, cursor: String?): EsItemFilter {

        return EsItemGenericFilter(
            cursor = cursor,
            creators = setOf(creator)
        )
    }

    fun getAllItemIdsByCollection(collectionId: String, cursor: String?): EsItemFilter {

        return EsItemGenericFilter(
            cursor = cursor,
            collections = setOf(collectionId)
        )
    }
}
