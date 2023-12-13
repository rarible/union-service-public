package com.rarible.protocol.union.enrichment.meta.collection.provider

import com.rarible.marketplace.generated.marketplacebackend.dto.TokenDto
import com.rarible.protocol.union.core.model.MetaSource
import com.rarible.protocol.union.core.model.UnionCollectionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.enrichment.download.ProviderDownloadException
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaMetrics
import com.rarible.protocol.union.enrichment.service.MarketplaceService
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class MarketplaceCollectionMetaProvider(
    private val marketplaceService: MarketplaceService,
    private val metrics: CollectionMetaMetrics
) : CollectionMetaProvider {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun getSource(): MetaSource = MetaSource.MARKETPLACE

    override suspend fun fetch(
        blockchain: BlockchainDto,
        id: String,
        original: UnionCollectionMeta?
    ): UnionCollectionMeta? {
        if (!marketplaceService.isSupported(blockchain)) {
            return original
        }

        // If we got meta from original source, we should use it - not MP meta
        if (!original?.content.isNullOrEmpty()) {
            return original
        }

        val meta = getCollection(blockchain, id)

        val current = original ?: UnionCollectionMeta("")
        return current.copy(
            name = meta.name, // To avoid some default names like "Untitled"
            description = meta.description,
            content = meta.pic?.let {
                listOf(UnionMetaContent(it, MetaContentDto.Representation.ORIGINAL))
            } ?: emptyList()
        )
    }

    private suspend fun getCollection(blockchain: BlockchainDto, id: String): TokenDto {
        val collectionId = CollectionIdDto(blockchain, id)
        return try {
            marketplaceService.getCollection(CollectionIdDto(blockchain, id))
        } catch (e: Exception) {
            metrics.onMetaError(blockchain, MetaSource.MARKETPLACE)
            logger.error("Failed to fetch from Marketplace $collectionId", e)
            throw ProviderDownloadException(MetaSource.MARKETPLACE)
        }
    }
}
