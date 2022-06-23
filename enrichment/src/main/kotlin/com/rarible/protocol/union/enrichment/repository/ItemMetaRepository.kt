package com.rarible.protocol.union.enrichment.repository

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadEntryRepository
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.DB)
class ItemMetaRepository(
    template: ReactiveMongoTemplate
) : DownloadEntryRepository<UnionMeta>(template) {

    override val collection = "enrichment_item_meta"
}