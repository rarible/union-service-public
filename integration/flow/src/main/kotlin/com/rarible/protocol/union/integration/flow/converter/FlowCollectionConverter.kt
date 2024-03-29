package com.rarible.protocol.union.integration.flow.converter

import com.rarible.protocol.dto.FlowCollectionEventDto
import com.rarible.protocol.dto.FlowCollectionUpdateEventDto
import com.rarible.protocol.dto.FlowNftCollectionDto
import com.rarible.protocol.dto.FlowNftCollectionsDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionCollectionUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.continuation.page.Page
import org.slf4j.LoggerFactory

object FlowCollectionConverter {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun convert(source: FlowNftCollectionDto, blockchain: BlockchainDto): UnionCollection {
        try {
            return convertInternal(source, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Collection: {} \n{}", blockchain, e.message, source)
            throw e
        }
    }

    private fun convertInternal(source: FlowNftCollectionDto, blockchain: BlockchainDto): UnionCollection {
        return UnionCollection(
            id = CollectionIdDto(blockchain, source.id),
            name = source.name,
            symbol = source.symbol,
            owner = UnionAddressConverter.convert(blockchain, source.owner),
            structure = UnionCollection.Structure.REGULAR,
            type = UnionCollection.Type.FLOW,
            features = convert(source.features),
            minters = null // Not supported in Flow yet.
        )
    }

    fun convert(page: FlowNftCollectionsDto, blockchain: BlockchainDto): Page<UnionCollection> {
        return Page(
            total = 0,
            continuation = page.continuation,
            entities = page.data.map { convert(it, blockchain) }
        )
    }

    fun convert(feature: FlowNftCollectionDto.Features): UnionCollection.Features {
        return when (feature) {
            FlowNftCollectionDto.Features.BURN -> UnionCollection.Features.BURN
            FlowNftCollectionDto.Features.SECONDARY_SALE_FEES -> UnionCollection.Features.SECONDARY_SALE_FEES
        }
    }

    fun convert(features: List<FlowNftCollectionDto.Features>): List<UnionCollection.Features> {
        return features.map(this::convert)
    }

    fun convert(event: FlowCollectionEventDto, blockchain: BlockchainDto): UnionCollectionEvent? {
        val marks = FlowConverter.convert(event.eventTimeMarks)
        return when (event) {
            is FlowCollectionUpdateEventDto -> {
                val unionCollection = convert(event.collection, blockchain)
                UnionCollectionUpdateEvent(unionCollection, marks)
            }
        }
    }
}
