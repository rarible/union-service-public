package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.core.logging.Logger
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexCollection
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexPage


object ImmutablexCollectionConverter {
    private val logger by Logger()

    fun convert(source: ImmutablexCollection): CollectionDto {
        try {
            return convertInternal(source)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Collection: {} \n{}", BlockchainDto.IMMUTABLEX, e.message, source)
            throw e
        }
    }

    private fun convertInternal(source: ImmutablexCollection): CollectionDto {
        return CollectionDto(
            id = CollectionIdDto(BlockchainDto.IMMUTABLEX, source.address),
            blockchain = BlockchainDto.IMMUTABLEX,
            name = source.name,
            symbol = source.name,
            owner = null,
            type = CollectionDto.Type.IMMUTABLEX,
            features = emptyList(),
            minters = null // Not supported
        )
    }

    fun convert(page: ImmutablexPage<ImmutablexCollection>): Page<CollectionDto> {
        return Page(
            total = page.result.size.toLong(),
            continuation = page.cursor,
            entities = page.result.map { convert(it) }
        )
    }

}