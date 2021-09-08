package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.FlowNftCollectionDto
import com.rarible.protocol.dto.FlowNftCollectionsDto
import com.rarible.protocol.union.dto.FlowBlockchainDto
import com.rarible.protocol.union.dto.FlowCollectionDto
import com.rarible.protocol.union.dto.UnionCollectionsDto

object FlowUnionCollectionConverter {

    fun convert(source: FlowNftCollectionDto, blockchain: FlowBlockchainDto): FlowCollectionDto {
        return FlowCollectionDto(
            id = FlowContractConverter.convert(source.id, blockchain),
            name = source.name,
            symbol = source.symbol,
            owner = FlowAddressConverter.convert(source.owner, blockchain)
        )
    }

    fun convert(page: FlowNftCollectionsDto, blockchain: FlowBlockchainDto): UnionCollectionsDto {
        return UnionCollectionsDto(
            total = page.total.toLong(), // TODO should be long
            continuation = page.continuation,
            collections = page.data.map { convert(it, blockchain) }
        )
    }


}

