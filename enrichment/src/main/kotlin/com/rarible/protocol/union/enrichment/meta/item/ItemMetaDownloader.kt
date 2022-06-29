package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.core.apm.CaptureTransaction
import com.rarible.loader.cache.CacheLoader
import com.rarible.loader.cache.CacheType
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.download.DownloadException
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.meta.downloader.Downloader
import org.springframework.stereotype.Component

@Component
class ItemMetaDownloader(
    private val itemMetaLoader: ItemMetaLoader
) : CacheLoader<UnionMeta>, Downloader<UnionMeta> {

    override val type
        get() = TYPE

    @CaptureTransaction
    override suspend fun download(id: String): UnionMeta {
        return try {
            load(id)
        } catch (e: Exception) {
            throw DownloadException(e.message ?: "Unexpected exception")
        }
    }

    @Deprecated("Should be replaced by download()")
    @CaptureTransaction
    override suspend fun load(key: String): UnionMeta {
        val itemId = IdParser.parseItemId(key)
        return itemMetaLoader.load(itemId)
            ?: throw UnionMetaResolutionException("No meta resolved for ${itemId.fullId()}")
    }

    companion object {

        const val TYPE: CacheType = "union-meta"
    }

    class UnionMetaResolutionException(message: String) : RuntimeException(message)

}
