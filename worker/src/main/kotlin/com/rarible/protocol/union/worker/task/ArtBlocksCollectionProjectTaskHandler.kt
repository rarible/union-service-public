package com.rarible.protocol.union.worker.task

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.enrichment.configuration.EnrichmentCollectionProperties
import com.rarible.protocol.union.enrichment.custom.collection.provider.ArtBlocksCustomCollectionProvider
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaPipeline
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaService
import com.rarible.protocol.union.enrichment.model.EnrichmentCollection
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.model.MetaDownloadPriority
import com.rarible.protocol.union.enrichment.util.optimisticLockWithInitial
import com.rarible.protocol.union.worker.job.AbstractBatchJob
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component
import org.web3j.crypto.Keys
import scalether.domain.Address
import java.util.concurrent.atomic.AtomicInteger

@Component
@Deprecated("Remove after execution")
class ArtBlocksCollectionProjectTaskHandler(
    private val job: ArtBlocksCollectionProjectJob
) : TaskHandler<String> {

    override val type = "ART_BLOCKS_COLLECTION_PROJECT_ID_TASK"

    override fun runLongTask(from: String?, param: String) = job.handle(from, param)
}

@Component
class ArtBlocksCollectionProjectJob(
    enrichmentCollectionProperties: EnrichmentCollectionProperties,
    private val collectionMetaService: CollectionMetaService,
    private val template: ReactiveMongoTemplate
) : AbstractBatchJob() {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val artBlocksCollections = enrichmentCollectionProperties.mappings
        .find { it.name == ArtBlocksCustomCollectionProvider.MAPPING_NAME }
        ?.getCollectionIds()
        ?.toSet() ?: emptySet()

    override suspend fun handleBatch(continuation: String?, param: String): String? {
        artBlocksCollections.forEach { handleChildren(it) }
        return null
    }

    private suspend fun handleChildren(parent: EnrichmentCollectionId) {
        logger.info("Updating children of ArtBlocks collection: {}", parent)
        val children = getChildren(parent).associateBy { it.id }.toMutableMap()
        val projectId = AtomicInteger(0)

        while (children.isNotEmpty()) {
            val subCollectionId = getSubCollectionId(parent, projectId.get())
            children.remove(subCollectionId)?.let { updateSubCollection(it, projectId.get()) }
            if (projectId.incrementAndGet() > 1000) {
                throw IllegalArgumentException(
                    "Reached projectId=${projectId.get()}, for {$parent} but there are still" +
                        "several children left: ${children.map { it.key }}"
                )
            }
        }
    }

    private suspend fun updateSubCollection(
        collection: EnrichmentCollection,
        projectId: Int
    ) {
        optimisticLockWithInitial(collection) {
            template.save(collection.copy(extra = mapOf("project_id" to projectId.toString())))
                .awaitSingle()
        }
        collectionMetaService.schedule(
            collectionId = collection.id.toDto(),
            pipeline = CollectionMetaPipeline.REFRESH,
            force = true,
            priority = MetaDownloadPriority.RIGHT_NOW
        )
        logger.info(
            "Updated ArtBlocks collection {} (parent={}) with projectId={}",
            collection.id,
            collection.parent,
            projectId
        )
    }

    private suspend fun getChildren(parent: EnrichmentCollectionId): List<EnrichmentCollection> {
        val query = Query(
            Criteria().andOperator(
                EnrichmentCollection::blockchain isEqualTo parent.blockchain, // Better performance without parent-index
                EnrichmentCollection::parent isEqualTo parent
            )
        )
        return template.find(query, EnrichmentCollection::class.java).collectList().awaitSingle()
    }

    private fun getSubCollectionId(parent: EnrichmentCollectionId, projectId: Int): EnrichmentCollectionId {
        val token = Address.apply(
            Keys.getAddress("custom_collection:artblocks:${parent.collectionId}:$projectId")
        ).prefixed()
        return EnrichmentCollectionId(parent.blockchain, token)
    }
}
