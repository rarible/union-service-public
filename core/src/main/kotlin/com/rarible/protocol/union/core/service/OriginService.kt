package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.core.model.Origin
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import org.springframework.stereotype.Component
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

@Component
class OriginService(
    orderServiceRouter: BlockchainRouter<OrderService>
) {

    private val collectionOrigins: MutableMap<CollectionIdDto, MutableSet<String>> = ConcurrentHashMap()
    private val globalOrigins: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())

    init {
        orderServiceRouter.getEnabledBlockchains().forEach { blockchain ->
            val origins = orderServiceRouter.getService(blockchain).getOrigins()
            origins.forEach { originProperties ->
                addOrigin(blockchain, originProperties)
            }
        }
    }

    private fun addOrigin(blockchain: BlockchainDto, origin: Origin) {
        if (origin.collections.isEmpty()) {
            globalOrigins.add(origin.origin)
        } else {
            addCollectionOrigins(blockchain, origin.origin, origin.collections)
        }
    }

    private fun addCollectionOrigins(blockchain: BlockchainDto, origin: String, collections: List<String>) {
        collections.forEach { collection ->
            val collectionId = CollectionIdDto(blockchain, collection)
            collectionOrigins.computeIfAbsent(collectionId) { Collections.newSetFromMap(ConcurrentHashMap()) }
                .add(origin)
        }
    }

    fun getOrigins(collectionId: CollectionIdDto?): List<String> {
        val collectionOrigins = collectionId?.let { collectionOrigins[collectionId] } ?: emptySet()
        return (globalOrigins + collectionOrigins).toList()
    }
}
