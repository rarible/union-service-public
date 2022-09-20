package com.rarible.protocol.union.integration.tezos.dipdup.converter

import com.rarible.dipdup.client.core.model.DipDupCollection
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.group
import org.slf4j.LoggerFactory

object DipDupCollectionConverter {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val blockchain = BlockchainDto.TEZOS

    fun convert(source: DipDupCollection): UnionCollection {
        try {
            return convertInternal(source)
        } catch (e: Exception) {
            logger.error("Failed to convert {} DipDup Collection: {} \n{}", blockchain, e.message, source)
            throw e
        }
    }

    private fun convertInternal(collection: DipDupCollection): UnionCollection {
        return UnionCollection(
            id = CollectionIdDto(blockchain, collection.id),
            name = collection.name,
            type = CollectionDto.Type.TEZOS_MT,
            owner = UnionAddress(blockchain.group(), collection.owner),
            minters = minters(collection),
            features = listOf(CollectionDto.Features.SECONDARY_SALE_FEES, CollectionDto.Features.BURN),
            symbol = collection.symbol
        )
    }

    private fun minters(source: DipDupCollection): List<UnionAddress> {
        val minters = source.minters.map { UnionAddress(blockchain.group(), it) }

        // We need to do that due to marketplace will ignore event without minters
        // When dipdup indexer sends the correct event with minters we can remove this fix
        return minters.ifEmpty { listOf(UnionAddress(blockchain.group(), source.owner)) }
    }
}
