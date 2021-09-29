package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.FlowNftCollectionDto
import com.rarible.protocol.dto.FlowNftCollectionsDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionsDto

object FlowCollectionConverter {

    fun convert(source: FlowNftCollectionDto, blockchain: BlockchainDto): CollectionDto {
        return CollectionDto(
            id = FlowContractConverter.convert(source.id, blockchain),
            name = source.name,
            symbol = source.symbol,
            owner = UnionAddressConverter.convert(source.owner, blockchain),
            type = CollectionDto.Type.FLOW
        )
    }

    fun convert(page: FlowNftCollectionsDto, blockchain: BlockchainDto): CollectionsDto {
        return CollectionsDto(
            total = page.total,
            continuation = page.continuation,
            collections = page.data.map { convert(it, blockchain) }
        )
    }


}

