package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.meta.downloader.DownloadService
import com.rarible.protocol.union.enrichment.repository.ItemMetaRepository
import org.springframework.stereotype.Component

@Component
class ItemMetaDownloadService(
    repository: ItemMetaRepository,
    publisher: ItemMetaTaskPublisher,
    downloader: ItemMetaDownloader,
    notifier: ItemMetaNotifier
) : DownloadService<ItemIdDto, UnionMeta>(repository, publisher, downloader, notifier) {

    override val type = "ITEM META"

    override fun toId(key: ItemIdDto): String {
        return key.fullId()
    }
}