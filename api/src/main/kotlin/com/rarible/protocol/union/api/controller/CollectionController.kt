package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.service.ItemApiService
import com.rarible.protocol.union.api.service.select.CollectionSourceSelectService
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionsDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.meta.UnionMetaService
import com.rarible.protocol.union.enrichment.model.ShortCollectionId
import com.rarible.protocol.union.enrichment.service.EnrichmentCollectionService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@ExperimentalCoroutinesApi
@RestController
class CollectionController(
    private val router: BlockchainRouter<CollectionService>,
    private val collectionSourceSelector: CollectionSourceSelectService,
    private val itemApiService: ItemApiService,
    private val unionMetaService: UnionMetaService,
    private val enrichmentCollectionService: EnrichmentCollectionService
) : CollectionControllerApi {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun getAllCollections(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<CollectionsDto> {
        val result = collectionSourceSelector.getAllCollections(blockchains, continuation, size)
        return ResponseEntity.ok(result)
    }

    override suspend fun getCollectionById(
        collection: String
    ): ResponseEntity<CollectionDto> {
        val fullCollectionId = IdParser.parseCollectionId(collection)
        val shortCollectionId = ShortCollectionId(fullCollectionId)
        val unionCollection = router.getService(fullCollectionId.blockchain).getCollectionById(fullCollectionId.value)
        val shortCollection = enrichmentCollectionService.get(shortCollectionId)
        val enrichedCollection = enrichmentCollectionService.enrichCollection(shortCollection, unionCollection)
        return ResponseEntity.ok(enrichedCollection)
    }

    override suspend fun refreshCollectionMeta(collection: String): ResponseEntity<Unit> {
        val collectionId = IdParser.parseCollectionId(collection)
        logger.info("Refreshing collection meta for '{}'", collection)
        router.getService(collectionId.blockchain).refreshCollectionMeta(collectionId.value)
        itemApiService.getAllItemIdsByCollection(collectionId).collect { unionMetaService.scheduleLoading(it) }
        return ResponseEntity.ok().build()
    }

    override suspend fun getCollectionsByOwner(
        owner: String,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<CollectionsDto> {
        val result = collectionSourceSelector.getCollectionsByOwner(owner, blockchains, continuation, size)
        return ResponseEntity.ok(result)
    }
}
