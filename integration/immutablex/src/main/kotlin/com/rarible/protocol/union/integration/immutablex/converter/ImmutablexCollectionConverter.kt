package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexCollection
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexPage


object ImmutablexCollectionConverter {
    private val logger by Logger()

    fun convert(source: ImmutablexCollection, blockchain: BlockchainDto = BlockchainDto.IMMUTABLEX): UnionCollection {
        try {
            return convertInternal(source, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Collection: {} \n{}", blockchain, e.message, source)
            throw e
        }
    }

    private fun convertInternal(source: ImmutablexCollection, blockchain: BlockchainDto): UnionCollection {
        return UnionCollection(
            id = CollectionIdDto(blockchain, source.address),
            name = source.name,
            symbol = source.name,
            owner = null,
            type = CollectionDto.Type.IMMUTABLEX,
            features = emptyList(),
            minters = emptyList()
        )
    }

    fun convert(page: ImmutablexPage<ImmutablexCollection>): Page<UnionCollection> {
        return Page(
            total = 0L,
            continuation = page.cursor,
            entities = page.result.map { convert(it) }
        )
    }

}
