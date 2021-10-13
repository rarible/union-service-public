package com.rarible.protocol.union.core.tezos.converter

import com.rarible.protocol.tezos.dto.ATFA_1_2Dto
import com.rarible.protocol.tezos.dto.ATFA_2Dto
import com.rarible.protocol.tezos.dto.ATXTZDto
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.TezosFA12AssetTypeDto
import com.rarible.protocol.union.dto.TezosFA2AssetTypeDto
import com.rarible.protocol.union.dto.TezosXTZAssetTypeDto
import com.rarible.protocol.union.dto.UnionAddress

object TezosAssetConverter {

    fun convert(source: com.rarible.protocol.tezos.dto.AssetDto, blockchain: BlockchainDto): AssetDto {
        return AssetDto(
            type = convertAssetType(source = source.assetType, blockchain = blockchain),
            value = source.value.toBigDecimal() //todo what is actual value here?
        )
    }

    private fun convertAssetType(source: com.rarible.protocol.tezos.dto.AssetTypeDto, blockchain: BlockchainDto): AssetTypeDto {
        return when (source) {
            is ATXTZDto ->
                TezosXTZAssetTypeDto()
            is ATFA_1_2Dto ->
                TezosFA12AssetTypeDto(
                    contract = UnionAddress(blockchain, source.contract)
                )
            is ATFA_2Dto ->
                TezosFA2AssetTypeDto(
                    contract = UnionAddress(blockchain, source.contract),
                    tokenId = source.tokenId
                )
        }
    }

}