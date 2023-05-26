package com.rarible.protocol.union.enrichment.custom.collection.mapper

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
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.EnumMap

@Component
class CollectionMapperIndex(
    private val itemServiceRouter: BlockchainRouter<ItemService>,
    private val itemRepository: ItemRepository,
    private val ff: FeatureFlagsProperties,
    collectionProperties: EnrichmentCollectionProperties,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    // It is better to have index allows us to obtain mapped collection with O(1) complexity
    private val index = EnumMap<BlockchainDto, CustomCollectionRuleIndex>(BlockchainDto::class.java)

    init {
        logger.info("Custom collection mapping provided: {}", collectionProperties.mappings)
        collectionProperties.mappings.forEach { rule -> indexRule(rule) }
    }

    fun getCollectionMapper(collectionId: CollectionIdDto): CollectionMapper? {
        return index[collectionId.blockchain]?.byCollection?.get(collectionId.value)
    }

    fun getItemMapping(itemId: ItemIdDto): CollectionIdDto? {
        return index[itemId.blockchain]?.byItem?.get(itemId.value)
    }

    private fun indexRule(rule: CustomCollectionMapping) {
        if (!ff.enableCustomCollections) {
            return
        }
        val customCollectionId = IdParser.parseCollectionId(rule.customCollection)
        rule.getItemIds().forEach {
            getBlockchainIndex(it.blockchain).byItem[it.itemId] = customCollectionId
        }

        rule.getCollectionIds().forEach {
            getBlockchainIndex(it.blockchain).addCollectionMapper(it, CollectionMapperDirect(customCollectionId))
        }

        rule.getRanges().forEach {
            getBlockchainIndex(it.collection.blockchain).addCollectionMapper(
                it.collection,
                CollectionMapperByTokenRange(customCollectionId, it.range)
            )
        }

        rule.meta.getCollectionIds().forEach {
            getBlockchainIndex(it.blockchain).addCollectionMapper(
                it,
                CollectionMapperByMeta(customCollectionId, rule.meta.getAttributes(), itemServiceRouter, itemRepository)
            )
        }
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

    private class CompositeCollectionMapper : CollectionMapper {

        private val mappers = ArrayList<CollectionMapper>()
        fun add(mapper: CollectionMapper) = mappers.add(mapper)

        override suspend fun getCustomCollections(
            itemIds: Collection<ItemIdDto>,
            hint: Map<ItemIdDto, ShortItem>
        ): Map<ItemIdDto, CollectionIdDto> {
            val result = HashMap<ItemIdDto, CollectionIdDto>()
            val temp = HashSet(itemIds)
            mappers.forEach {
                val mapped = it.getCustomCollections(temp, hint)
                result.putAll(mapped)
                temp.removeAll(mapped.keys)
            }
            return result
        }

        override suspend fun getCustomCollections(
            collectionIds: Collection<CollectionIdDto>
        ): Map<CollectionIdDto, CollectionIdDto> {
            val result = HashMap<CollectionIdDto, CollectionIdDto>()
            val temp = HashSet(collectionIds)
            mappers.forEach {
                val mapped = it.getCustomCollections(temp)
                result.putAll(mapped)
                temp.removeAll(mapped.keys)
            }
            return result
        }
    }

}