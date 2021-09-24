package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.ItemRoyaltyDto
import com.rarible.protocol.dto.ItemTransferDto
import com.rarible.protocol.dto.NftItemAttributeDto
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.dto.NftItemsDto
import com.rarible.protocol.dto.NftMediaDto
import com.rarible.protocol.dto.NftMediaMetaDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.EthItemDto
import com.rarible.protocol.union.dto.EthItemRoyaltyDto
import com.rarible.protocol.union.dto.EthItemTransferDto
import com.rarible.protocol.union.dto.UnionItemIdDto
import com.rarible.protocol.union.dto.UnionItemsDto
import com.rarible.protocol.union.dto.UnionMetaAttributeDto
import com.rarible.protocol.union.dto.UnionMetaContentDto
import com.rarible.protocol.union.dto.UnionMetaDto

object EthUnionItemConverter {

    fun convert(item: NftItemDto, blockchain: BlockchainDto): EthItemDto {
        return EthItemDto(
            id = UnionItemIdDto(
                token = UnionAddressConverter.convert(item.contract, blockchain),
                tokenId = item.tokenId,
                blockchain = blockchain
            ),
            mintedAt = item.date ?: nowMillis(), // TODO RPN-848
            lastUpdatedAt = item.date ?: nowMillis(),
            supply = item.supply,
            metaUrl = null, //TODO
            meta = item.meta?.let { convert(it) },
            deleted = item.deleted ?: false,
            tokenId = item.tokenId,
            collection = UnionAddressConverter.convert(item.contract, blockchain),
            creators = item.creators.map { EthConverter.convertToCreator(it, blockchain) },
            owners = item.owners.map { UnionAddressConverter.convert(it, blockchain) },
            royalties = item.royalties.map { EthConverter.convertToRoyalty(it, blockchain) },
            lazySupply = item.lazySupply,
            pending = item.pending?.map { convert(it, blockchain) } ?: listOf()
        )
    }

    fun convert(page: NftItemsDto, blockchain: BlockchainDto): UnionItemsDto {
        return UnionItemsDto(
            total = page.total,
            continuation = page.continuation,
            items = page.items.map { convert(it, blockchain) }
        )
    }

    fun convert(source: ItemTransferDto, blockchain: BlockchainDto): EthItemTransferDto {
        return EthItemTransferDto(
            owner = UnionAddressConverter.convert(source.owner, blockchain),
            contract = UnionAddressConverter.convert(source.contract, blockchain),
            tokenId = source.tokenId,
            value = source.value,
            date = source.date,
            from = UnionAddressConverter.convert(source.from, blockchain)
        )
    }

    fun convert(source: ItemRoyaltyDto, blockchain: BlockchainDto): EthItemRoyaltyDto {
        return EthItemRoyaltyDto(
            owner = source.owner?.let { UnionAddressConverter.convert(it, blockchain) },
            contract = UnionAddressConverter.convert(source.contract, blockchain),
            tokenId = source.tokenId,
            value = source.value!!,
            date = source.date,
            royalties = source.royalties.map { EthConverter.convertToRoyalty(it, blockchain) }
        )
    }

    private fun convert(source: NftItemMetaDto): UnionMetaDto {
        return UnionMetaDto(
            name = source.name,
            description = source.description,
            attributes = source.attributes?.filter { it.value != null }?.map { convert(it) } ?: listOf(),
            contents = listOfNotNull(source.image, source.animation).flatMap { convert(it) },
            raw = null //TODO
        )
    }

    private fun convert(source: NftMediaDto): List<UnionMetaContentDto> {
        return source.url.map { urlMap ->
            val type = urlMap.key
            val url = urlMap.value

            UnionMetaContentDto(
                typeContent = type,
                url = url,
                attributes = source.meta[type]?.let { convert(it) } ?: listOf()
            )
        }
    }

    private fun convert(source: NftMediaMetaDto): List<UnionMetaAttributeDto> {
        return listOfNotNull(
            UnionMetaAttributeDto(
                key = NftMediaMetaDto::type.name,
                value = source.type
            ),
            source.height?.let {
                UnionMetaAttributeDto(
                    key = NftMediaMetaDto::height.name,
                    value = it.toString()
                )
            },
            source.width?.let {
                UnionMetaAttributeDto(
                    key = NftMediaMetaDto::width.name,
                    value = it.toString()
                )
            }
        )
    }

    private fun convert(source: NftItemAttributeDto): UnionMetaAttributeDto {
        return UnionMetaAttributeDto(
            key = source.key,
            value = source.value!!
        )
    }
}

