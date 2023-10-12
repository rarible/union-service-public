package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.protocol.dto.BurnLazyNftFormDto
import com.rarible.protocol.dto.LazyErc1155Dto
import com.rarible.protocol.dto.LazyErc721Dto
import com.rarible.protocol.dto.LazyNftDto
import com.rarible.protocol.union.core.exception.UnionValidationException
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.EthLazyItemErc1155Dto
import com.rarible.protocol.union.dto.EthLazyItemErc721Dto
import com.rarible.protocol.union.dto.LazyItemBurnFormDto
import com.rarible.protocol.union.dto.LazyItemMintFormDto

object UnionItemConverter {

    fun convert(source: LazyItemMintFormDto): LazyNftDto {
        val dto = source.item
        val (contract, tokenId) = CompositeItemIdParser.split(dto.id.value)
        val contractAddress = EthConverter.convertToAddress(contract)
        val creators = dto.creators.map { EthConverter.convertToPart(it) }
        val royalties = dto.royalties.map { EthConverter.convertToPart(it) }
        val signatures = dto.signatures.map(EthConverter::convertToBinary)
        return when (dto) {
            is EthLazyItemErc721Dto -> LazyErc721Dto(
                contract = contractAddress,
                tokenId = tokenId,
                uri = dto.uri,
                creators = creators,
                royalties = royalties,
                signatures = signatures,
            )

            is EthLazyItemErc1155Dto -> LazyErc1155Dto(
                contract = contractAddress,
                tokenId = tokenId,
                uri = dto.uri,
                creators = creators,
                royalties = royalties,
                signatures = signatures,
                supply = dto.supply
            )

            else -> throw UnionValidationException(
                "Lazy Item of type ${dto.javaClass.simpleName} is not supported by this blockchain," +
                    "use EthLazyItemErc721 or EthLazyItemErc1155"
            )
        }
    }

    fun convert(source: LazyItemBurnFormDto): BurnLazyNftFormDto {
        return BurnLazyNftFormDto(
            creators = source.creators.map { EthConverter.convertToAddress(it.value) },
            signatures = source.signatures.map(EthConverter::convertToBinary)
        )
    }
}
