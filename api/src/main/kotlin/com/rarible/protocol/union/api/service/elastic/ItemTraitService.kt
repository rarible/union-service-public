package com.rarible.protocol.union.api.service.elastic

import com.rarible.protocol.union.api.configuration.EsProperties
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.elastic.EsTraitFilter
import com.rarible.protocol.union.core.model.trait.TraitProperty
import com.rarible.protocol.union.dto.ExtendedTraitPropertyDto
import com.rarible.protocol.union.dto.TraitsDto
import com.rarible.protocol.union.enrichment.converter.TraitConverter
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.repository.search.EsTraitRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component

@Component
class ItemTraitService(
    private val legacyItemTraitService: LegacyItemTraitService,
    private val esTraitRepository: EsTraitRepository,
    private val esItemRepository: EsItemRepository,
    private val ff: FeatureFlagsProperties,
    private val esProperties: EsProperties,
) {
    suspend fun getTraitsWithRarity(
        collectionId: String,
        properties: Set<TraitProperty>
    ): List<ExtendedTraitPropertyDto> = coroutineScope {
        if (ff.enableOptimizedSearchForTraits) {
            val itemsCount = async { esItemRepository.countItemsInCollection(collectionId) }
            val result = async { esTraitRepository.getTraits(collectionId, properties) }
            TraitConverter.convertWithRarity(result.await(), itemsCount.await())
        } else {
            legacyItemTraitService.getTraitsWithRarity(collectionId, properties).map { it.toApiDto() }
        }
    }

    suspend fun searchTraits(filter: String, collectionIds: List<String>): TraitsDto {
        return if (ff.enableOptimizedSearchForTraits) {
            searchTraits(
                EsTraitFilter(
                    text = filter,
                    collectionIds = collectionIds.toSet(),
                    keysLimit = esProperties.itemsTraitsKeysLimit,
                    valuesLimit = esProperties.itemsTraitsValuesLimit,
                )
            )
        } else {
            legacyItemTraitService.searchTraits(filter, collectionIds)
        }
    }

    suspend fun queryTraits(collectionIds: List<String>, keys: List<String>?): TraitsDto {
        return if (ff.enableOptimizedSearchForTraits) {
            searchTraits(
                EsTraitFilter(
                    keys = keys?.toSet() ?: emptySet(),
                    collectionIds = collectionIds.toSet(),
                    keysLimit = esProperties.itemsTraitsKeysLimit,
                    valuesLimit = esProperties.itemsTraitsValuesLimit,
                )
            )
        } else {
            legacyItemTraitService.queryTraits(collectionIds, keys)
        }
    }

    private suspend fun searchTraits(esFilter: EsTraitFilter): TraitsDto {
        val result = esTraitRepository.searchTraits(esFilter)
        return TraitConverter.convert(result)
    }
}
