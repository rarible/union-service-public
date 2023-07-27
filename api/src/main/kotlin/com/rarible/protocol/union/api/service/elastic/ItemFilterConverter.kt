package com.rarible.protocol.union.api.service.elastic

import com.rarible.protocol.union.core.model.elastic.EsItemFilter
import com.rarible.protocol.union.core.model.elastic.EsItemGenericFilter
import com.rarible.protocol.union.core.model.elastic.TraitFilter
import com.rarible.protocol.union.dto.ItemsSearchFilterDto
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ItemFilterConverter() {

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

    fun getItemsByCreator(creator: String, blockchains: Set<String>?, cursor: String?): EsItemFilter {

        return EsItemGenericFilter(
            cursor = cursor,
            creators = setOf(creator),
            blockchains = blockchains
        )
    }

    fun getAllItemIdsByCollection(collectionId: String, cursor: String?): EsItemFilter {

        return EsItemGenericFilter(
            cursor = cursor,
            collections = setOf(collectionId)
        )
    }

    fun searchItems(filter: ItemsSearchFilterDto, cursor: String?): EsItemFilter {
        return EsItemGenericFilter(
            cursor = cursor,
            blockchains = filter.blockchains?.map { it.name }?.toSet(),
            collections = filter.collections?.map { it.fullId() }?.toSet(),
            creators = filter.creators?.let { addresses -> addresses.map { it.fullId() }.toSet() }.orEmpty(),
            mintedFrom = filter.mintedAtFrom,
            mintedTo = filter.mintedAtTo,
            updatedFrom = filter.lastUpdatedAtFrom,
            updatedTo = filter.lastUpdatedAtTo,
            deleted = filter.deleted,
            descriptions = filter.descriptions?.toSet(),
            traits = filter.traits?.map { TraitFilter(it.key, it.value) },
            sellPlatforms = filter.sellPlatforms?.map { it.name }?.toSet(),
            sellPriceCurrency = filter.sellCurrency,
            sellPriceFrom = filter.sellPriceFrom,
            sellPriceTo = filter.sellPriceTo,
            bidPlatforms = filter.bidPlatforms?.map { it.name }?.toSet(),
            bidPriceCurrency = filter.bidCurrency,
            bidPriceFrom = filter.bidPriceFrom,
            bidPriceTo = filter.bidPriceTo,
        )
    }
}
