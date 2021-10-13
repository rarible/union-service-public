package com.rarible.protocol.union.core.tezos.converter

import com.rarible.protocol.tezos.dto.NftCollectionDto
import com.rarible.protocol.tezos.dto.NftCollectionsDto
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.UnionAddress

object TezosCollectionConverter {

    fun convert(source: NftCollectionDto, blockchain: BlockchainDto): CollectionDto {
        return CollectionDto(
            id = UnionAddress(blockchain, source.id),
            name = source.name,
            symbol = source.symbol,
            owner = source.owner?.let { UnionAddress(blockchain, it) },
            type = CollectionDto.Type.TEZOS
        )
    }

    fun convert(page: NftCollectionsDto, blockchain: BlockchainDto): Page<CollectionDto> {
        return Page(
            total = page.total.toLong(),
            continuation = page.continuation,
            entities = page.collections.map { convert(it, blockchain) }
        )
    }

}

