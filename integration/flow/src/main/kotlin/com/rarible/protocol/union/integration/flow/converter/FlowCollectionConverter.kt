package com.rarible.protocol.union.integration.flow.converter

import com.rarible.protocol.dto.FlowNftCollectionDto
import com.rarible.protocol.dto.FlowNftCollectionsDto
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto

object FlowCollectionConverter {

    fun convert(source: FlowNftCollectionDto, blockchain: BlockchainDto): CollectionDto {
        return CollectionDto(
            id = UnionAddressConverter.convert(source.id, blockchain),
            name = source.name,
            symbol = source.symbol,
            owner = UnionAddressConverter.convert(source.owner, blockchain),
            type = CollectionDto.Type.FLOW
        )
    }

    fun convert(page: FlowNftCollectionsDto, blockchain: BlockchainDto): Page<CollectionDto> {
        return Page(
            total = page.total,
            continuation = page.continuation,
            entities = page.data.map { convert(it, blockchain) }
        )
    }


}

