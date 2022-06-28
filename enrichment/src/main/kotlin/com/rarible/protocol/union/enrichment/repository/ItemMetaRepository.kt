package com.rarible.protocol.union.enrichment.repository

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadEntryRepository
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.stereotype.Component

@Component
// TODO CaptureSpan breaks bean creation
//@CaptureSpan(type = SpanType.DB)
class ItemMetaRepository(
    template: ReactiveMongoTemplate
) : DownloadEntryRepository<UnionMeta>(
    template,
    "enrichment_item_meta"
)