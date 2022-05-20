package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.core.DefaultBlockchainProperties
import com.rarible.protocol.union.core.OriginProperties
import com.rarible.protocol.union.core.util.safeSplit
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import org.springframework.stereotype.Component

@Component
class OriginService(
    properties: List<DefaultBlockchainProperties>
) {

    private val collectionOrigins: MutableMap<CollectionIdDto, MutableSet<String>> = HashMap()
    private val globalOrigins: MutableSet<String> = HashSet()

    init {
        properties.forEach {
            val blockchain = it.blockchain
            val origins = it.origins
            origins.values.forEach { originProperties ->
                addOrigin(blockchain, originProperties)
            }
        }
    }

    private fun addOrigin(blockchain: BlockchainDto, properties: OriginProperties) {
        val collections = safeSplit(properties.collections)
        val origin = properties.origin
        if (collections.isEmpty()) {
            globalOrigins.add(origin)
        } else {
            addCollectionOrigins(blockchain, origin, collections)
        }
    }

    private fun addCollectionOrigins(blockchain: BlockchainDto, origin: String, collections: List<String>) {
        collections.forEach { collection ->
            val collectionId = CollectionIdDto(blockchain, collection)
            collectionOrigins.computeIfAbsent(collectionId) { HashSet() }.add(origin)
        }
    }

    fun getOrigins(collectionId: CollectionIdDto): Set<String> {
        return globalOrigins + (collectionOrigins[collectionId] ?: emptySet())
    }

}