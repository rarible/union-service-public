package com.rarible.protocol.union.api.service.elastic

import com.rarible.protocol.union.core.model.EsItemCursor
import com.rarible.protocol.union.core.model.EsItemFilter
import com.rarible.protocol.union.core.model.EsItemGenericFilter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemsSearchFilterDto
import com.rarible.protocol.union.dto.continuation.CombinedContinuation
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import java.time.Instant
import org.springframework.stereotype.Service

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

    fun searchItems(filter: ItemsSearchFilterDto, cursor: EsItemCursor?): ElasticItemFilter {
        return ElasticItemFilter(
            cursor = cursor,
            blockchains = filter.blockchains?.map { it.name }?.toSet(),
            collections = filter.collections?.map { it.fullId() }?.toSet(),
            creators = filter.creators?.map { it.fullId() }?.toSet(),
            mintedFrom = filter.mintedAtFrom,
            mintedTo = filter.mintedAtTo,
            updatedFrom = filter.lastUpdatedAtFrom,
            updatedTo = filter.lastUpdatedAtTo,
            deleted = filter.deleted,
            descriptions = filter.descriptions?.toSet(),
            traitsKeys = filter.traits?.map { it.key }?.toSet(),
            traitsValues = filter.traits?.map { it.value }?.toSet()
        )
    }
}
