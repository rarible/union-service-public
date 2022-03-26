package com.rarible.protocol.union.integration.solana.converter

import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.SolanaFtAssetTypeDto
import com.rarible.protocol.union.dto.SolanaNftAssetTypeDto
import com.rarible.solana.protocol.dto.TokenAssetTypeDto

object SolanaConverter {
    fun convert(source: com.rarible.solana.protocol.dto.AssetDto): AssetDto {
        return AssetDto(
            type = convert(source = source.type),
            value = source.value
        )
    }

    fun convert(source: com.rarible.solana.protocol.dto.AssetTypeDto): AssetTypeDto =
        when (source) {
            is TokenAssetTypeDto -> if (source.isNft) {
                SolanaNftAssetTypeDto(source.mint)
            } else {
                SolanaFtAssetTypeDto(source.mint)
            }
        }


}