package com.rarible.protocol.union.listener.downloader

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.enrichment.repository.ItemMetaRepository
import org.springframework.stereotype.Component

@Component
class ItemMetaTaskScheduler(
    router: ItemMetaTaskRouter,
    repository: ItemMetaRepository
) : DownloadScheduler<UnionMeta>(router, repository)