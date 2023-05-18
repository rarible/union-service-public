package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.configuration.CustomCollectionMapping
import com.rarible.protocol.union.enrichment.configuration.EnrichmentCollectionProperties
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigInteger
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

    // TODO ideally, there should be checks for:
    // 1. There is no duplicated mappings (for items and collections)
    // 2. Same collection/item can't be mapped to different custom collections
    // 3. There is no intersections in ranges
    // 4. There is no conflicts with ranges and direct collection mappings
    private fun indexRule(rule: CustomCollectionMapping) {
        if (!ff.enableCustomCollections) {
            return
        }
        val customCollectionId = IdParser.parseCollectionId(rule.customCollection)
        rule.getItemIds().forEach {
            getBlockchainIndex(it.blockchain).byItem[it.itemId] = customCollectionId
        }

        rule.getCollectionIds().forEach {
            getBlockchainIndex(it.blockchain).addCollectionMapper(it, DirectCollectionMapper(customCollectionId))
        }

        rule.getRanges().forEach {
            getBlockchainIndex(it.collection.blockchain).addCollectionMapper(
                it.collection,
                TokenIdRangeCollectionMapper(customCollectionId, it.range)
            )
        }
    }

    suspend fun resolveCustomCollection(itemId: ItemIdDto): CollectionIdDto? {
        val blockchainIndex = index[itemId.blockchain] ?: return null
        blockchainIndex.byItem[itemId.value]?.let { return it }

        // Collection ID without blockchain prefix - as it mapped in our index
        val collectionId = itemServiceRouter.getService(itemId.blockchain)
            .getItemCollectionId(itemId.value) ?: return null // Possible only for Solana

        return blockchainIndex.byCollection[collectionId]?.getCollectionId(itemId)
    }

    suspend fun resolveCustomCollection(collectionId: CollectionIdDto): CollectionIdDto? {
        val blockchainIndex = index[collectionId.blockchain] ?: return null

        return blockchainIndex.byCollection[collectionId.value]?.getCollectionId(null)
    }

    private fun getBlockchainIndex(blockchain: BlockchainDto): CustomCollectionRuleIndex {
        return index.computeIfAbsent(blockchain) { CustomCollectionRuleIndex() }
    }

    private class CustomCollectionRuleIndex {

        // Mapped WITHOUT blockchain prefix
        val byItem = HashMap<String, CollectionIdDto>()
        val byCollection = HashMap<String, CompositeCollectionMapper>()

        fun addCollectionMapper(collectionId: EnrichmentCollectionId, mapper: CollectionMapper) {
            byCollection.computeIfAbsent(collectionId.collectionId) { CompositeCollectionMapper() }
                .add(mapper)
        }

    }

    private interface CollectionMapper {

        fun getCollectionId(itemId: ItemIdDto?): CollectionIdDto?
    }

    private class DirectCollectionMapper(private val collectionId: CollectionIdDto) : CollectionMapper {

        override fun getCollectionId(itemId: ItemIdDto?) = collectionId
    }

    private class TokenIdRangeCollectionMapper(
        private val collectionId: CollectionIdDto,
        private val range: ClosedRange<BigInteger>
    ) : CollectionMapper {

        override fun getCollectionId(itemId: ItemIdDto?): CollectionIdDto? {
            // Doesn't work for solana
            if (itemId == null || itemId.blockchain == BlockchainDto.SOLANA) {
                return null
            }
            val tokenId = CompositeItemIdParser.split(itemId.value).second
            return if (tokenId in range) collectionId else null
        }
    }

    private class CompositeCollectionMapper() {

        private val mappers = ArrayList<CollectionMapper>()
        fun add(mapper: CollectionMapper) = mappers.add(mapper)

        fun getCollectionId(itemId: ItemIdDto?): CollectionIdDto? {
            mappers.forEach { mapper -> mapper.getCollectionId(itemId)?.let { return it } }
            return null
        }

    }

}