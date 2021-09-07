package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.*
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.dto.ethereum.EthItemIdProvider

object EthUnionItemConverter {

    fun convert(item: NftItemDto, blockchain: EthBlockchainDto): EthItemDto {
        return EthItemDto(
            id = EthItemIdProvider.create(
                token = EthAddressConverter.convert(item.contract, blockchain),
                tokenId = item.tokenId,
                blockchain = blockchain
            ),
            mintedAt = item.date ?: nowMillis(),
            lastUpdatedAt = item.date ?: nowMillis(),
            supply = item.supply,
            metaURL = null, //TODO
            meta = item.meta?.let { convert(it) },
            deleted = item.deleted ?: false,
            tokenId = item.tokenId,
            collection = EthAddressConverter.convert(item.contract, blockchain),
            creators = item.creators.map { EthConverter.convertToCreator(it, blockchain) },
            owners = item.owners.map { EthAddressConverter.convert(it, blockchain) },
            royalties = item.royalties.map { EthConverter.convertToRoyalty(it, blockchain) },
            lazySupply = item.lazySupply,
            pending = item.pending?.map { convert(it, blockchain) }
        )
    }

    fun convert(items: NftItemsDto, blockchain: EthBlockchainDto): UnionItemsDto {
        return UnionItemsDto(
            total = items.total,
            continuation = items.continuation,
            items = items.items.map { convert(it, blockchain) }
        )
    }

    private fun convert(source: NftItemMetaDto): UnionMetaDto {
        return UnionMetaDto(
            name = source.name,
            description = source.description,
            attributes = source.attributes?.map { convert(it) },
            contents = listOfNotNull(source.image, source.animation).flatMap { convert(it) },
            raw = null //TODO
        )
    }

    private fun convert(source: NftMediaDto): List<UnionMetaContentDto> {
        return source.url.map { urlMap ->
            val type = urlMap.value
            val url = urlMap.key

            UnionMetaContentDto(
                typeContent = type,
                url = url,
                attributes = source.meta[type]?.let { convert(it) }
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
            value = source.value ?: "" //TODO
        )
    }

    private fun convert(source: ItemTransferDto, blockchain: EthBlockchainDto): EthPendingItemDto {
        return EthPendingItemDto(
            type = EthPendingItemDto.Type.TRANSFER,
            from = EthAddressConverter.convert(source.from, blockchain)
        )
    }
}

