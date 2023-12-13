package com.rarible.protocol.union.enrichment.service

import com.rarible.marketplace.generated.marketplacebackend.api.client.CollectionControllerApi
import com.rarible.marketplace.generated.marketplacebackend.dto.TokenDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.enrichment.configuration.CommonMetaProperties
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service

@Service
class MarketplaceService(
    private val marketplaceCollectionControllerApi: CollectionControllerApi,
    metaProperties: CommonMetaProperties
) {

    private val properties = metaProperties.marketplace

    suspend fun getCollection(id: CollectionIdDto): TokenDto {
        val mpId = "${id.blockchain}-${id.value}"
        return marketplaceCollectionControllerApi.getCollectionUsingGET(mpId).awaitSingle()
    }

    fun isSupported(blockchain: BlockchainDto): Boolean {
        return blockchain in properties.supported && properties.enabled
    }
}
