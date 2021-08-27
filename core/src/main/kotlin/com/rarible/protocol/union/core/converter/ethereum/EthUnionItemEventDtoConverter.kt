package com.rarible.protocol.union.core.converter.ethereum

import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.*
import com.rarible.protocol.union.dto.*
import java.time.Instant

object EthUnionItemEventDtoConverter {
    fun convert(source: NftItemEventDto, blockchain: Blockchain): UnionItemEventDto {
        return when (source) {
            is NftItemUpdateEventDto -> UnionItemUpdateEventDto(
                eventId = source.eventId,
                itemId = ItemId(source.itemId),
                item = EthItemDto(
                    mintedAt = source.item.date ?: Instant.now(),
                    lastUpdatedAt = source.item.date ?: Instant.now(),
                    supply = source.item.supply,
                    metaURL = null, //TODO
                    blockchain  = convert(blockchain),
                    meta = source.item.meta?.let { convert(it) },
                    deleted = source.item.deleted ?: false,
                    id = EthItemId(source.item.id),
                    tokenId = source.item.tokenId,
                    collection  = EthAddressConverter.convert(source.item.contract),
                    creators = source.item.creators.map { EthCreatorDtoConverter.convert(it) },
                    owners = source.item.owners.map { EthAddressConverter.convert(it) },
                    royalties = source.item.royalties.map { EthRoyaltyDtoConverter.convert(it) },
                    lazySupply = source.item.lazySupply,
                    pending = source.item.pending?.map { convert(it) }
                )
            )
            is NftItemDeleteEventDto -> UnionItemDeleteEventDto(
                eventId = source.eventId,
                itemId = ItemId(source.eventId) //TODO: Need typed itemId
            )
        }
    }

    private fun convert(source: NftItemMetaDto): MetaDto {
        return MetaDto(
            name = source.name,
            description = source.description,
            attributes = source.attributes?.map { convert(it) },
            contents = listOfNotNull(source.image, source.animation).flatMap { convert(it) },
            raw = null //TODO
        )
    }

    private fun convert(source: NftItemAttributeDto): MetaAttributeDto {
        return MetaAttributeDto(
            key = source.key,
            value = source.value ?: ""
        )
    }

    private fun convert(source: ItemTransferDto): EthPendingItemDto {
        return EthPendingItemDto(
            type = EthPendingItemDto.Type.TRANSFER,
            from = EthAddressConverter.convert(source.from)
        )
    }

    private fun convert(source: NftMediaDto): List<MetaContentDto> {
        return source.url.map { urlMap ->
            val type = urlMap.value
            val url = urlMap.key

            MetaContentDto(
                typeContent = type,
                url = url,
                attributes = source.meta[type]?.let { convert(it) }
            )
        }
    }

    private fun convert(source: NftMediaMetaDto): List<MetaAttributeDto> {
        return listOfNotNull(
            MetaAttributeDto(
                key = NftMediaMetaDto::type.name,
                value = source.type
            ),
            source.height?.let {
                MetaAttributeDto(
                    key = NftMediaMetaDto::height.name,
                    value = it.toString()
                )
            },
            source.width?.let {
                MetaAttributeDto(
                    key = NftMediaMetaDto::width.name,
                    value = it.toString()
                )
            }
        )
    }

    private fun convert(source: Blockchain): EthBlockchainDto {
        return when (source) {
            Blockchain.ETHEREUM -> EthBlockchainDto.ETHEREUM
            Blockchain.POLYGON -> EthBlockchainDto.POLYGON
        }
    }
}

