package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.dto.NftCollectionMetaDto
import com.rarible.protocol.dto.NftCollectionsDto
import com.rarible.protocol.dto.NftMediaDto
import com.rarible.protocol.dto.NftMediaMetaDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CollectionMetaDto
import com.rarible.protocol.union.dto.ImageContentDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.page.Page
import org.slf4j.LoggerFactory
import scalether.domain.Address

object EthCollectionConverter {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun convert(source: NftCollectionDto, blockchain: BlockchainDto): CollectionDto {
        try {
            return convertInternal(source, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Collection: {} \n{}", blockchain, e.message, source)
            throw e
        }
    }

    private fun convertInternal(source: NftCollectionDto, blockchain: BlockchainDto): CollectionDto {
        val contract = EthConverter.convert(source.id)
        return CollectionDto(
            id = CollectionIdDto(blockchain, contract),
            blockchain = blockchain,
            name = source.name,
            symbol = source.symbol,
            type = convert(source.type),
            owner = source.owner?.let { EthConverter.convert(it, blockchain) },
            features = source.features.map { convert(it) },
            minters = source.minters?.let { minters -> minters.map { EthConverter.convert(it, blockchain) } },
            meta = convert(source.meta),
        )
    }

    fun convert(page: NftCollectionsDto, blockchain: BlockchainDto): Page<CollectionDto> {
        return Page(
            total = page.total,
            continuation = page.continuation,
            entities = page.collections.map { convert(it, blockchain) }
        )
    }

    private fun convert(type: NftCollectionDto.Type): CollectionDto.Type {
        return when (type) {
            NftCollectionDto.Type.ERC721 -> CollectionDto.Type.ERC721
            NftCollectionDto.Type.ERC1155 -> CollectionDto.Type.ERC1155
            NftCollectionDto.Type.CRYPTO_PUNKS -> CollectionDto.Type.CRYPTO_PUNKS
        }
    }

    private fun convert(feature: NftCollectionDto.Features): CollectionDto.Features {
        return when (feature) {
            NftCollectionDto.Features.APPROVE_FOR_ALL -> CollectionDto.Features.APPROVE_FOR_ALL
            NftCollectionDto.Features.BURN -> CollectionDto.Features.BURN
            NftCollectionDto.Features.MINT_AND_TRANSFER -> CollectionDto.Features.MINT_AND_TRANSFER
            NftCollectionDto.Features.MINT_WITH_ADDRESS -> CollectionDto.Features.MINT_WITH_ADDRESS
            NftCollectionDto.Features.SECONDARY_SALE_FEES -> CollectionDto.Features.SECONDARY_SALE_FEES
            NftCollectionDto.Features.SET_URI_PREFIX -> CollectionDto.Features.SET_URI_PREFIX
        }
    }

    private fun convert(source: NftCollectionMetaDto?): CollectionMetaDto? {
        if (source == null) return null
        return CollectionMetaDto(
            name = source.name,
            description = source.description,
            content = convert(source.image),
            externalLink = source.external_link,
            sellerFeeBasisPoints = source.seller_fee_basis_points,
            feeRecipient = convert(source.fee_recipient),
        )
    }

    // Assuming the input NtfMediaDto is an image
    private fun convert(sourceImage: NftMediaDto?): List<MetaContentDto> {
        if (sourceImage == null) return emptyList()
        return sourceImage.url.keys.map { key ->
            convert(key, sourceImage.url[key]!!, sourceImage.meta[key])
        }
    }

    private fun convert(key: String, url: String, meta: NftMediaMetaDto?): MetaContentDto {
        return ImageContentDto(
            url = url,
            representation = MetaContentDto.Representation.valueOf(key),
            mimeType = meta?.type,
            width = meta?.width,
            height = meta?.height,
            size = null, // TODO find where to get size from
        )
    }

    private fun convert(source: Address?): UnionAddress? {
        if (source == null) return null
        return UnionAddress(
            blockchainGroup = BlockchainGroupDto.ETHEREUM,
            value = EthConverter.convert(source)
        )
    }
}
