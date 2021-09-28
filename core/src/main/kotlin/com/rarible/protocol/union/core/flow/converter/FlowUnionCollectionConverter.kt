package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.FlowNftCollectionDto
import com.rarible.protocol.dto.FlowNftCollectionsDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionCollectionDto
import com.rarible.protocol.union.dto.UnionCollectionsDto

object FlowUnionCollectionConverter {

    fun convert(source: FlowNftCollectionDto, blockchain: BlockchainDto): UnionCollectionDto {
        return UnionCollectionDto(
            id = FlowContractConverter.convert(source.id, blockchain),
            name = source.name,
            symbol = source.symbol,
            owner = UnionAddressConverter.convert(source.owner, blockchain),
            type = UnionCollectionDto.Type.FLOW
        )
    }

    fun convert(page: FlowNftCollectionsDto, blockchain: BlockchainDto): UnionCollectionsDto {
        return UnionCollectionsDto(
            total = page.total,
            continuation = page.continuation,
            collections = page.data.map { convert(it, blockchain) }
        )
    }


}

