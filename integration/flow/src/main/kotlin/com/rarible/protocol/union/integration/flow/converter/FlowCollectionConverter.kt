package com.rarible.protocol.union.integration.flow.converter

import com.rarible.protocol.dto.FlowNftCollectionDto
import com.rarible.protocol.dto.FlowNftCollectionsDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.continuation.page.Page
import org.slf4j.LoggerFactory

object FlowCollectionConverter {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun convert(source: FlowNftCollectionDto, blockchain: BlockchainDto): CollectionDto {
        try {
            return convertInternal(source, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Collection: {} \n{}", blockchain, e.message, source)
            throw e
        }
    }

    private fun convertInternal(source: FlowNftCollectionDto, blockchain: BlockchainDto): CollectionDto {
        return CollectionDto(
            id = CollectionIdDto(blockchain, source.id),
            blockchain = blockchain,
            name = source.name,
            symbol = source.symbol,
            owner = UnionAddressConverter.convert(blockchain, source.owner),
            type = CollectionDto.Type.FLOW,
            features = convert(source.features),
            minters = null // Not supported in Flow yet.
        )
    }

    fun convert(page: FlowNftCollectionsDto, blockchain: BlockchainDto): Page<CollectionDto> {
        return Page(
            total = page.total,
            continuation = page.continuation,
            entities = page.data.map { convert(it, blockchain) }
        )
    }

    fun convert(feature: FlowNftCollectionDto.Features): CollectionDto.Features {
        return when(feature) {
            FlowNftCollectionDto.Features.BURN -> CollectionDto.Features.BURN
            FlowNftCollectionDto.Features.SECONDARY_SALE_FEES -> CollectionDto.Features.SECONDARY_SALE_FEES
        }
    }

    fun convert(features: List<FlowNftCollectionDto.Features>): List<CollectionDto.Features> {
        return features.map(this::convert)
    }

}
