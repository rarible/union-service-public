package com.rarible.protocol.union.core.tezos.converter

import com.rarible.protocol.tezos.dto.NftItemDto
import com.rarible.protocol.tezos.dto.NftItemsDto
import com.rarible.protocol.tezos.dto.PartDto
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.UnionItemDto

object TezosItemConverter {

    fun convert(item: NftItemDto, blockchain: BlockchainDto): UnionItemDto {
        return UnionItemDto(
            id = ItemIdDto(
                blockchain = blockchain,
                token = UnionAddress(blockchain, item.contract),
                tokenId = item.tokenId
            ),
            tokenId = item.tokenId,
            collection = UnionAddress(blockchain, item.contract),
            creators = item.creators.map { toCreator(it, blockchain) },
            deleted = item.deleted ?: false, //todo raise to tezos
            lastUpdatedAt = item.date,
            lazySupply = item.lazySupply,
            meta = null, //todo we can set probably here
            mintedAt = item.date, //todo ask tezos to include
            owners = item.owners.map { UnionAddress(blockchain, it) },
            pending = emptyList(), //todo tezos better delete this if they don't populate it,
            royalties = item.royalties.map { toRoyalty(it, blockchain) },
            supply = item.supply
        )
    }

    fun convert(page: NftItemsDto, blockchain: BlockchainDto): Page<UnionItemDto> {
        return Page(
            total = page.total.toLong(),
            continuation = page.continuation,
            entities = page.items.map { convert(it, blockchain) }
        )
    }

    private fun toRoyalty(
        source: PartDto,
        blockchain: BlockchainDto
    ): RoyaltyDto {
        return RoyaltyDto(
            account = UnionAddressConverter.convert(source.account, blockchain),
            value = source.value.toBigDecimal()
        )
    }

    private fun toCreator(
        source: PartDto,
        blockchain: BlockchainDto
    ): CreatorDto {
        return CreatorDto(
            account = UnionAddressConverter.convert(source.account, blockchain),
            value = source.value.toBigDecimal()
        )
    }
}