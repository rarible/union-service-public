package com.rarible.protocol.union.enrichment.repository

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadEntryRepository
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.index.Index
import org.springframework.stereotype.Component

@Component
// TODO CaptureSpan breaks bean creation
//@CaptureSpan(type = SpanType.DB)
class ItemMetaRepository(
    template: ReactiveMongoTemplate
) : DownloadEntryRepository<UnionMeta>(
    template,
    "enrichment_item_meta"
) {
    private val logger = LoggerFactory.getLogger(ItemMetaRepository::class.java)

    suspend fun createIndices() {
        ALL_INDEXES.forEach { index ->
            logger.info("Ensure index '{}' for collection '{}'", index, collection)
            template.indexOps(collection).ensureIndex(index).awaitFirst()
        }
    }

    companion object {

        private val COLLECTION_DEFINITION = Index()
            .on(UnionMeta::collectionId.name, Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .background()

        private val ALL_INDEXES = listOf(
            COLLECTION_DEFINITION
        )
    }
}