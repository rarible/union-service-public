package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.configuration.UnionMetaProperties
import com.rarible.protocol.union.enrichment.meta.MetaSource
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaMetrics
import com.rarible.protocol.union.enrichment.meta.simplehash.SimpleHashConverter
import com.rarible.protocol.union.enrichment.meta.simplehash.SimpleHashItem
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigInteger

@Service
@ConditionalOnProperty("meta.simpleHash.enabled", havingValue = "true")
class SimpleHashService(
    private val props: UnionMetaProperties,
    private val simpleHashClient: WebClient,
    private val metrics: ItemMetaMetrics,
    private val itemServiceRouter: BlockchainRouter<ItemService>,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun isSupported(blockchain: BlockchainDto) : Boolean {
        return blockchain in props.simpleHash.supported
    }

    suspend fun fetch(key: ItemIdDto): UnionMeta? {
        try {
            if (isLazy(key)) {
                logger.info("Skipped to fetch from simplehash $key because it's lazy item")
            } else {
                val response = simpleHashClient.get()
                    .uri("/nfts/${network(key.blockchain)}/${key.value.replace(":", "/")}")
                    .retrieve().bodyToMono(SimpleHashItem::class.java).awaitSingle()
                metrics.onMetaFetched(key.blockchain, MetaSource.SIMPLE_HASH)
                return SimpleHashConverter.convert(response)
            }
        } catch (e: Exception) {
            metrics.onMetaError(key.blockchain, MetaSource.SIMPLE_HASH)
            logger.error("Failed to fetch from simplehash $key", e)
        }
        return null
    }

    suspend fun fetch(key: CollectionIdDto): UnionMeta? {
        // TODO: request from simplehash
        return null
    }

    private fun network(blockchain: BlockchainDto): String {
        return props.simpleHash.mapping[blockchain.name.lowercase()] ?: blockchain.name.lowercase()
    }

    private suspend fun isLazy(key: ItemIdDto): Boolean {
        val item = itemServiceRouter.getService(key.blockchain).getItemById(key.value)
        return item?.lazySupply > BigInteger.ZERO
    }

}
