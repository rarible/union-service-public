package com.rarible.protocol.union.enrichment.custom.collection.mapper

import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory

class CollectionMapperByMeta(
    private val collectionId: CollectionIdDto,
    // attributeName -> acceptableValues
    private val attributes: Map<String, Set<String>>,
    private val itemServiceRouter: BlockchainRouter<ItemService>,
    private val itemRepository: ItemRepository,
) : CollectionMapper {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun getCustomCollections(
        itemIds: Collection<ItemIdDto>,
        hint: Map<ItemIdDto, ShortItem>
    ): Map<ItemIdDto, CollectionIdDto> {
        val missing = itemIds.filter { !hint.containsKey(it) }

        val fromDb = itemRepository.getAll(missing.map { ShortItemId(it) })
            .filter { it.metaEntry?.data != null }
            .associateBy({ it.id.toDto() }, { it.metaEntry!!.data!! })

        val missingFromDb = missing.filter { fromDb.containsKey(it) }
        val fromBlockchain = fetchMeta(missingFromDb)
        val fromHint = hint.filter { it.value.metaEntry?.data != null }
            .mapValues { it.value.metaEntry!!.data!! }

        return (fromDb + fromBlockchain + fromHint)
            .filter { matches(it.value) }
            .mapValues { collectionId }
    }

    private suspend fun fetchMeta(itemIds: Collection<ItemIdDto>): Map<ItemIdDto, UnionMeta> {
        if (itemIds.isEmpty()) {
            return emptyMap()
        }
        logger.warn("Fetching meta to determine custom collections for Items: {}", itemIds.map { it.fullId() })
        // TODO Ideally it SHOULD NOT be used
        return coroutineScope {
            itemIds.chunked(16).map { chunk ->
                chunk.map {
                    async {
                        try {
                            it to itemServiceRouter.getService(it.blockchain).getItemMetaById(it.value)
                        } catch (e: Exception) {
                            logger.warn(
                                "Failed to fetch meta to determine custom collection of Item: {}",
                                it.fullId()
                            )
                            null
                        }
                    }
                }.awaitAll().filterNotNull()
            }
        }.flatten().associateBy({ it.first }, { it.second })
    }

    private fun matches(meta: UnionMeta): Boolean {
        meta.attributes.forEach { attr ->
            attributes[attr.key]?.let {
                if (it.contains(attr.value)) return true
            }
        }
        return false
    }

}