package com.rarible.protocol.union.enrichment.meta.content

import com.rarible.protocol.union.core.model.ContentOwner
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.meta.MetaCustomizer
import com.rarible.protocol.union.enrichment.util.sanitizeContent

abstract class MetaContentEnrichmentService<K, T : ContentOwner<T>>(
    private val contentMetaLoader: ContentMetaDownloader,
    private val customizers: List<MetaCustomizer<K, T>>,
) {

    suspend fun enrcih(key: K, meta: T): T {
        val sanitized = sanitizeContent(meta.content)
        val (_, blockchain) = extractBlockchain(key)
        val content = contentMetaLoader.enrichContent(key.toString(), blockchain, sanitized)
        val initial = meta.withContent(content)
        return customizers.fold(initial) { current, customizer ->
            customizer.customize(key, current)
        }
    }

    abstract fun extractBlockchain(key: K): Pair<String, BlockchainDto>
}
