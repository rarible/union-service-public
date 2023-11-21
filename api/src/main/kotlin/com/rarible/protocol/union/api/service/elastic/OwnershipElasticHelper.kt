package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.common.mapAsync
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.model.elastic.EsOwnership
import com.rarible.protocol.union.core.model.elastic.EsOwnershipByCollectionFilter
import com.rarible.protocol.union.core.model.elastic.EsOwnershipByItemFilter
import com.rarible.protocol.union.core.model.elastic.EsOwnershipByOwnerFilter
import com.rarible.protocol.union.core.model.elastic.EsOwnershipSort
import com.rarible.protocol.union.core.model.elastic.EsOwnershipsSearchFilter
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipSearchRequestDto
import com.rarible.protocol.union.dto.OwnershipSearchSortDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.dto.parser.OwnershipIdParser
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OwnershipElasticHelper(
    private val repository: EsOwnershipRepository,
    private val esOwnershipOptimizedSearchService: EsOwnershipOptimizedSearchService,
    private val router: BlockchainRouter<OwnershipService>,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getRawOwnershipsByOwner(
        owner: UnionAddress,
        blockchains: Set<BlockchainDto>,
        continuation: String?,
        size: Int
    ): Slice<UnionOwnership> {
        val filter = EsOwnershipByOwnerFilter(owner, blockchains, cursor = continuation)
        val ownerships = repository.search(filter, EsOwnershipSort.DEFAULT, size)
        return Slice(
            continuation = ownerships.continuation,
            entities = getOwnerships(ownerships.entities)
        )
    }

    suspend fun getRawOwnershipsByCollection(collectionId: CollectionIdDto, continuation: String?, size: Int): Slice<UnionOwnership> {
        val filter = EsOwnershipByCollectionFilter(collectionId, cursor = continuation)
        val ownerships = repository.search(filter, EsOwnershipSort.DEFAULT, size)
        return Slice(
            continuation = ownerships.continuation,
            entities = getOwnerships(ownerships.entities)
        )
    }

    suspend fun getRawOwnershipsByItem(itemId: ItemIdDto, continuation: String?, size: Int): Slice<UnionOwnership> {
        val filter = EsOwnershipByItemFilter(itemId, cursor = continuation)
        val ownerships = repository.search(filter, EsOwnershipSort.DEFAULT, size)
        return Slice(
            continuation = ownerships.continuation,
            entities = getOwnerships(ownerships.entities)
        )
    }

    suspend fun getRawOwnershipsBySearchRequest(request: OwnershipSearchRequestDto): Slice<UnionOwnership> {
        val filter = EsOwnershipsSearchFilter(request)
        val sort = convertSort(request.sort)
        val ownerships = esOwnershipOptimizedSearchService.search(filter, sort, request.size)
        return Slice(
            continuation = ownerships.continuation,
            entities = getOwnerships(ownerships.entities)
        )
    }

    private fun convertSort(sort: OwnershipSearchSortDto?): EsOwnershipSort {
        if (sort == null) return EsOwnershipSort.DEFAULT
        return when (sort) {
            OwnershipSearchSortDto.LATEST -> EsOwnershipSort.LATEST_FIRST
            OwnershipSearchSortDto.EARLIEST -> EsOwnershipSort.EARLIEST_FIRST
            OwnershipSearchSortDto.HIGHEST_SELL -> EsOwnershipSort.HIGHEST_SELL_PRICE_FIRST
            OwnershipSearchSortDto.LOWEST_SELL -> EsOwnershipSort.LOWEST_SELL_PRICE_FIRST
        }
    }

    private suspend fun getOwnerships(esOwnerships: List<EsOwnership>): List<UnionOwnership> {
        if (esOwnerships.isEmpty()) return emptyList()
        logger.debug("Enrich elastic ownerships with blockchains api (count=${esOwnerships.size})")
        val mapping = hashMapOf<BlockchainDto, MutableList<String>>()
        esOwnerships.forEach { ownership ->
            mapping
                .computeIfAbsent(ownership.blockchain) { ArrayList(esOwnerships.size) }
                .add(OwnershipIdParser.parseFull(ownership.id).value)
        }

        val ownerships = mapping.mapAsync { (blockchain, ids) ->
            val isBlockchainEnabled = router.isBlockchainEnabled(blockchain)
            if (isBlockchainEnabled) router.getService(blockchain).getOwnershipsByIds(ids) else emptyList()
        }.flatten()

        val ownershipsIdMapping = ownerships.associateBy { it.id.fullId().lowercase() }
        return esOwnerships.mapNotNull { esOwnership -> ownershipsIdMapping[esOwnership.id.lowercase()] }
    }
}
