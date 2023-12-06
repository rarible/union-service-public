package com.rarible.protocol.union.enrichment.meta.collection.provider

import com.rarible.protocol.union.core.model.MetaSource
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.enrichment.download.ProviderDownloadException
import com.rarible.protocol.union.enrichment.service.SimpleHashService
import org.springframework.stereotype.Component

@Component
class SimpleHashCollectionMetaProvider(
    private val simpleHashService: SimpleHashService,
) : CollectionMetaProvider {
    override fun getSource(): MetaSource = MetaSource.SIMPLE_HASH

    override suspend fun fetch(
        blockchain: BlockchainDto,
        id: String,
        original: UnionCollectionMeta?
    ): UnionCollectionMeta? {
        if (original != null || !simpleHashService.isSupportedCollection(blockchain)) {
            return original
        }
        val meta = simpleHashService.fetch(CollectionIdDto(blockchain = blockchain, value = id))
        if (meta == null || meta.content.isEmpty()) {
            throw ProviderDownloadException(MetaSource.SIMPLE_HASH)
        }
        return meta
    }
}
