package com.rarible.protocol.union.integration.solana.converter

import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.SolanaNftAssetTypeDto
import com.rarible.protocol.union.dto.SolanaSolAssetTypeDto

object SolanaConverter {

    fun convert(source: com.rarible.protocol.solana.dto.AssetDto): AssetDto {
        return AssetDto(
            type = convert(source = source.type),
            value = source.value
        )
    }

    fun convert(source: com.rarible.protocol.solana.dto.AssetTypeDto): AssetTypeDto =
        when (source) {
            is com.rarible.protocol.solana.dto.SolanaNftAssetTypeDto -> SolanaNftAssetTypeDto(
                itemId = ItemIdDto(BlockchainDto.SOLANA, source.mint)
            )
            is com.rarible.protocol.solana.dto.SolanaSolAssetTypeDto -> SolanaSolAssetTypeDto()
        }

    fun convert(source: ActivitySortDto): com.rarible.protocol.solana.dto.ActivitySortDto {
        return when (source) {
            ActivitySortDto.LATEST_FIRST -> com.rarible.protocol.solana.dto.ActivitySortDto.LATEST_FIRST
            ActivitySortDto.EARLIEST_FIRST -> com.rarible.protocol.solana.dto.ActivitySortDto.EARLIEST_FIRST
        }
    }

}