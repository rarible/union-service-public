package com.rarible.protocol.union.api.service.elastic

import com.rarible.protocol.union.core.exception.UnionException
import com.rarible.protocol.union.core.model.elastic.EsItemFilter
import com.rarible.protocol.union.core.model.elastic.EsItemGenericFilter
import com.rarible.protocol.union.core.model.elastic.FullTextSearch
import com.rarible.protocol.union.core.model.elastic.Range
import com.rarible.protocol.union.core.model.elastic.TextField
import com.rarible.protocol.union.core.model.elastic.TraitFilter
import com.rarible.protocol.union.core.model.elastic.TraitRangeFilter
import com.rarible.protocol.union.dto.ItemSearchFullTextDto
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

    fun searchItems(filter: ItemsSearchFilterDto, cursor: String?): EsItemFilter =
        EsItemGenericFilter(
            cursor = cursor,
            blockchains = filter.blockchains?.map { it.name }?.toSet(),
            collections = filter.collections?.map { it.fullId() }?.toSet(),
            creators = filter.creators?.map { it.fullId() }?.toSet().orEmpty(),
            mintedFrom = filter.mintedAtFrom,
            mintedTo = filter.mintedAtTo,
            updatedFrom = filter.lastUpdatedAtFrom,
            updatedTo = filter.lastUpdatedAtTo,
            deleted = filter.deleted,
            fullText = filter.fullText?.let { FullTextSearch(it.text, textFields(it)) },
            names = filter.names?.toSet(),
            traits = filter.traits?.map { TraitFilter(it.key, it.value) },
            traitRanges = filter.traitRanges?.map {
                TraitRangeFilter(key = it.key, valueRange = Range(from = it.valueRange.from, to = it.valueRange.to))
            },
            sellPlatforms = filter.sellPlatforms?.map { it.name }?.toSet(),
            sellPriceCurrency = filter.sellCurrency,
            sellPriceFrom = filter.sellPriceFrom,
            sellPriceTo = filter.sellPriceTo,
            bidPlatforms = filter.bidPlatforms?.map { it.name }?.toSet(),
            bidPriceCurrency = filter.bidCurrency,
            bidPriceFrom = filter.bidPriceFrom,
            bidPriceTo = filter.bidPriceTo,
            onSale = filter.onSale
        )

    private fun textFields(textDto: ItemSearchFullTextDto) =
        textDto.fields?.map { textField ->
            when (textField.name.uppercase()) {
                TextField.NAME.name -> TextField.NAME
                TextField.DESCRIPTION.name -> TextField.DESCRIPTION
                TextField.TRAIT_VALUE.name -> TextField.TRAIT_VALUE
                else -> throw UnionException("Unknown full text search field ${textField.name}, " +
                    "allowed values: ${TextField.values().map { it.toString() }}"
                )
            }
        } ?: listOf(TextField.NAME)
}
