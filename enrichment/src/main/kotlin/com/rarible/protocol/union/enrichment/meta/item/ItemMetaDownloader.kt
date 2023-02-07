package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadException
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.meta.downloader.Downloader
import org.springframework.stereotype.Component

@Component
class ItemMetaDownloader(
    private val itemMetaLoader: ItemMetaLoader
) : Downloader<UnionMeta> {

    @CaptureTransaction
    override suspend fun download(id: String): UnionMeta {
        val result = try {
            val itemId = IdParser.parseItemId(id)
            itemMetaLoader.load(itemId)
        } catch (e: Exception) {
            throw DownloadException(e.message ?: "Unexpected exception")
        }
        result ?: throw DownloadException("No meta resolved for $id")
        return result
    }
}
