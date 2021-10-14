package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ImageContentDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.dto.MetaDto
import com.rarible.protocol.union.dto.VideoContentDto
import com.rarible.protocol.union.enrichment.meta.ContentMetaService
import com.rarible.protocol.union.enrichment.model.ShortItemId
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component

@Component
class EnrichmentMetaService(
    private val router: BlockchainRouter<ItemService>,
    private val contentMetaService: ContentMetaService
) {
    suspend fun enrichMeta(meta: MetaDto?, itemId: ShortItemId): MetaDto? {
        val metaToEnrich = meta ?: router.getService(itemId.blockchain).getItemMetaById(itemId.toDto().value)
        return enrichMeta(metaToEnrich)
    }

    private suspend fun enrichMeta(meta: MetaDto): MetaDto = coroutineScope {
        val enrichedContents = meta.content.map {
            async {
                when (it) {
                    is VideoContentDto -> it.enrich()
                    is ImageContentDto -> it.enrich()
                }
            }
        }.awaitAll()
        meta.copy(content = enrichedContents)
    }

    private suspend fun ImageContentDto.enrich(): ImageContentDto {
        if (width != null || height != null) {
            return this
        }
        val contentMeta = contentMetaService.getContentMeta(url)
        return copy(width = contentMeta?.width, height = contentMeta?.height)
    }


    private suspend fun VideoContentDto.enrich(): VideoContentDto {
        if (width != null || height != null || duration != null) {
            return this
        }
        val contentMeta = contentMetaService.getContentMeta(url)
        return copy(width = contentMeta?.width, height = contentMeta?.height)
    }

}
