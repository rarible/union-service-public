package com.rarible.protocol.union.enrichment.repository

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadEntry
import com.rarible.protocol.union.core.model.download.DownloadStatus
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadEntryRepository
import com.rarible.protocol.union.enrichment.model.ShortItemId
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
    template: ReactiveMongoTemplate,
    private val itemRepository: ItemRepository,
) : DownloadEntryRepository<UnionMeta>(
    template,
    "enrichment_item_meta"
) {
    private val logger = LoggerFactory.getLogger(ItemMetaRepository::class.java)

    override suspend fun onSave(entry: DownloadEntry<UnionMeta>) {
        if (entry.status == DownloadStatus.SUCCESS) {
            itemRepository.updateLastUpdatedAt(ShortItemId(IdParser.parseItemId(entry.id)))
        }
    }

    suspend fun createIndices() {
        ALL_INDEXES.forEach { index ->
            logger.info("Ensure index '{}' for collection '{}'", index, collection)
            template.indexOps(collection).ensureIndex(index).awaitFirst()
        }
    }

    companion object {

        private val COLLECTION_DEFINITION = Index()
            .on("${DownloadEntry<UnionMeta>::data.name}.${UnionMeta::collectionId.name}", Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .background()

        private val ALL_INDEXES = listOf(
            COLLECTION_DEFINITION
        )
    }
}