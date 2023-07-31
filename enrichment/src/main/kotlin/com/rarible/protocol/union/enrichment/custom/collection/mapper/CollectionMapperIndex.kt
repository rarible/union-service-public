package com.rarible.protocol.union.enrichment.custom.collection.mapper

import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.configuration.CustomCollectionMapping
import com.rarible.protocol.union.enrichment.configuration.EnrichmentCollectionProperties
import com.rarible.protocol.union.enrichment.custom.collection.CustomCollectionItemProvider
import com.rarible.protocol.union.enrichment.custom.collection.provider.CustomCollectionProvider
import com.rarible.protocol.union.enrichment.custom.collection.provider.CustomCollectionProviderFactory
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.model.ShortItem
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.EnumMap

@Component
class CollectionMapperIndex(
    private val customCollectionItemProvider: CustomCollectionItemProvider,
    private val customCollectionProviderFactory: CustomCollectionProviderFactory,
    private val ff: FeatureFlagsProperties,
    collectionProperties: EnrichmentCollectionProperties,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    // It is better to have index allows us to obtain mapped collection with O(1) complexity
    private val index = EnumMap<BlockchainDto, CustomCollectionRuleIndex>(BlockchainDto::class.java)

    init {
        logger.info("Custom collection mapping provided: {}", collectionProperties.mappings)
        collectionProperties.mappings
            .filter { it.enabled }
            .forEach { rule -> indexRule(rule) }
    }

    fun getCollectionMapper(collectionId: CollectionIdDto): CollectionMapper? {
        return index[collectionId.blockchain]?.byCollection?.get(collectionId.value)
    }

    fun getItemProvider(itemId: ItemIdDto): CustomCollectionProvider? {
        return index[itemId.blockchain]?.byItem?.get(itemId.value)
    }

    private fun indexRule(mapping: CustomCollectionMapping) {
        if (!ff.enableCustomCollections) {
            return
        }
        val provider = customCollectionProviderFactory.create(mapping)
        mapping.getItemIds().forEach {
            getBlockchainIndex(it.blockchain).byItem[it.itemId] = provider
        }

        mapping.getCollectionIds().forEach {
            getBlockchainIndex(it.blockchain).addCollectionMapper(it, CollectionMapperDirect(provider))
        }

        mapping.getRanges().forEach {
            getBlockchainIndex(it.collection.blockchain).addCollectionMapper(
                it.collection,
                CollectionMapperByTokenRange(provider, it.range)
            )
        }

        mapping.meta.getCollectionIds().forEach {
            getBlockchainIndex(it.blockchain).addCollectionMapper(
                it,
                CollectionMapperByMeta(provider, mapping.meta.getAttributes(), customCollectionItemProvider)
            )
        }
    }

    private fun getBlockchainIndex(blockchain: BlockchainDto): CustomCollectionRuleIndex {
        return index.computeIfAbsent(blockchain) { CustomCollectionRuleIndex() }
    }

    private class CustomCollectionRuleIndex {

        // Mapped WITHOUT blockchain prefix
        val byItem = HashMap<String, CustomCollectionProvider>()
        val byCollection = HashMap<String, CompositeCollectionMapper>()

        fun addCollectionMapper(collectionId: EnrichmentCollectionId, mapper: CollectionMapper) {
            byCollection.computeIfAbsent(collectionId.collectionId) { CompositeCollectionMapper() }
                .add(mapper)
        }
    }

    private class CompositeCollectionMapper : CollectionMapper {

        private val mappers = ArrayList<CollectionMapper>()
        fun add(mapper: CollectionMapper) = mappers.add(mapper)

        override suspend fun getCustomCollectionProviders(
            itemIds: Collection<ItemIdDto>,
            hint: Map<ItemIdDto, ShortItem>
        ): Map<ItemIdDto, CustomCollectionProvider> {
            val result = HashMap<ItemIdDto, CustomCollectionProvider>()
            val temp = HashSet(itemIds)
            mappers.forEach {
                val mapped = it.getCustomCollectionProviders(temp, hint)
                result.putAll(mapped)
                temp.removeAll(mapped.keys)
            }
            return result
        }

        override suspend fun getCustomCollectionProviders(
            collectionIds: Collection<CollectionIdDto>
        ): Map<CollectionIdDto, CustomCollectionProvider> {
            val result = HashMap<CollectionIdDto, CustomCollectionProvider>()
            val temp = HashSet(collectionIds)
            mappers.forEach {
                val mapped = it.getCustomCollectionProviders(temp)
                result.putAll(mapped)
                temp.removeAll(mapped.keys)
            }
            return result
        }
    }
}
