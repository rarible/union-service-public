package com.rarible.protocol.union.enrichment.meta.content

import com.rarible.protocol.union.core.model.ContentOwner
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.meta.MetaCustomizer
import com.rarible.protocol.union.enrichment.meta.WrappedMeta
import com.rarible.protocol.union.enrichment.util.sanitizeContent

abstract class MetaContentEnrichmentService<K, T : ContentOwner<T>>(
    private val contentMetaLoader: ContentMetaDownloader,
    private val customizers: List<MetaCustomizer<K, T>>,
) {

    suspend fun enrcih(key: K, meta: WrappedMeta<T>): T {
        val sanitized = sanitizeContent(meta.data.content)
        val (id, blockchain) = generaliseKey(key)
        val content = contentMetaLoader.enrichContent(id, blockchain, sanitized)
        val initial = meta.copy(data = meta.data.withContent(content))
        return customizers.fold(initial) { current, customizer ->
            customizer.customize(key, current)
        }.data
    }

    abstract fun generaliseKey(key: K): Pair<String, BlockchainDto>
}