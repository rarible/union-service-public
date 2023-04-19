package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.configuration.CustomCollectionMapping
import com.rarible.protocol.union.enrichment.configuration.EnrichmentCollectionProperties
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.model.ShortItemId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.EnumMap

@Component
class CustomCollectionResolver(
    private val itemServiceRouter: BlockchainRouter<ItemService>,
    collectionProperties: EnrichmentCollectionProperties,
    private val ff: FeatureFlagsProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    // It is better to have index allows us to obtain mapped collection with O(1) complexity
    private val index = EnumMap<BlockchainDto, CustomCollectionRuleIndex>(BlockchainDto::class.java)

    init {
        logger.info("Custom collection mapping provided: {}", collectionProperties.mappings)
        collectionProperties.mappings.forEach { rule -> indexRule(rule) }
    }

    private fun indexRule(rule: CustomCollectionMapping) {
        if (!ff.enableCustomCollections) {
            return
        }
        val customCollectionId = IdParser.parseCollectionId(rule.customCollection)
        rule.collections.forEach { rawCollectionId ->
            val collectionId = EnrichmentCollectionId.of(rawCollectionId)
            index.computeIfAbsent(collectionId.blockchain) { CustomCollectionRuleIndex() }
                .byCollection[collectionId.collectionId] = customCollectionId
        }
        rule.items.forEach { rawItemId ->
            val itemId = ShortItemId.of(rawItemId)
            index.computeIfAbsent(itemId.blockchain) { CustomCollectionRuleIndex() }
                .byItem[itemId.itemId] = customCollectionId
        }
    }

    suspend fun resolveCustomCollection(itemId: ItemIdDto): CollectionIdDto? {
        val blockchainIndex = index[itemId.blockchain] ?: return null
        blockchainIndex.byItem[itemId.value]?.let { return it }

        // Collection ID without blockchain prefix - as it mapped in our index
        val collectionId = itemServiceRouter.getService(itemId.blockchain)
            .getItemCollectionId(itemId.value) ?: return null // Possible only for Solana

        return blockchainIndex.byCollection[collectionId]
    }

    suspend fun resolveCustomCollection(collectionId: CollectionIdDto): CollectionIdDto? {
        val blockchainIndex = index[collectionId.blockchain] ?: return null

        return blockchainIndex.byCollection[collectionId.value]
    }

    private class CustomCollectionRuleIndex {

        // Mapped WITHOUT blockchain prefix
        val byItem = HashMap<String, CollectionIdDto>()
        val byCollection = HashMap<String, CollectionIdDto>()
        // TODO potentially here we can add byRange
    }

}