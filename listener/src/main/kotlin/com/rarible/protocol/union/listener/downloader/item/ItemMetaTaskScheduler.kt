package com.rarible.protocol.union.listener.downloader.item

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.enrichment.repository.ItemMetaRepository
import com.rarible.protocol.union.listener.downloader.DownloadScheduler
import org.springframework.stereotype.Component

@Component
class ItemMetaTaskScheduler(
    router: ItemMetaTaskRouter,
    repository: ItemMetaRepository
) : DownloadScheduler<UnionMeta>(router, repository)