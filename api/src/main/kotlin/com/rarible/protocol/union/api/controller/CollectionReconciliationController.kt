package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.CollectionsDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.model.ShortCollectionId
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentCollectionService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class CollectionReconciliationController(
    private val router: BlockchainRouter<CollectionService>,
    private val enrichmentCollectionService: EnrichmentCollectionService,
    private val collectionRepository: CollectionRepository,
) {

    @GetMapping(value = ["/reconciliation/collections"], produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun getCollections(
        @RequestParam lastUpdatedFrom: Long,
        @RequestParam lastUpdatedTo: Long,
        @RequestParam(required = false) continuation: String? = null,
    ): CollectionsDto {
        val shortCollections = collectionRepository.findIdsByLastUpdatedAt(
            lastUpdatedFrom = Instant.ofEpochMilli(lastUpdatedFrom),
            lastUpdatedTo = Instant.ofEpochMilli(lastUpdatedTo),
            continuation = continuation?.let { ShortCollectionId(IdParser.parseCollectionId(continuation)) }
        )
        if (shortCollections.isEmpty()) {
            return CollectionsDto()
        }
        val groupedIds = shortCollections.groupBy({ it.blockchain }, { it.id.collectionId })

        val unionCollections = groupedIds.flatMap {
            router.getService(it.key).getCollectionsByIds(it.value)
        }.groupBy { it.id }

        val collections = shortCollections.mapNotNull { shortCollection ->
            val unionCollection = unionCollections[shortCollection.id.toDto()]
            if (unionCollection.isNullOrEmpty()) {
                null
            } else {
                enrichmentCollectionService.enrichCollection(shortCollection, unionCollection[0])
            }
        }

        return CollectionsDto(
            total = 0,
            collections = collections,
            continuation = shortCollections.last().id.toDto().fullId()
        )
    }
}