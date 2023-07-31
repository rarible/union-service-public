package com.rarible.protocol.union.integration.solana.converter

import com.rarible.protocol.union.core.model.UnionAsset
import com.rarible.protocol.union.core.model.UnionAssetType
import com.rarible.protocol.union.core.model.UnionSolanaFtAssetType
import com.rarible.protocol.union.core.model.UnionSolanaNftAssetType
import com.rarible.protocol.union.core.model.UnionSolanaSolAssetType
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.SolanaFtAssetTypeDto
import com.rarible.protocol.union.dto.SolanaNftAssetTypeDto
import com.rarible.protocol.union.dto.SolanaSolAssetTypeDto

object SolanaConverter {

    fun convert(source: com.rarible.protocol.solana.dto.AssetDto, blockchain: BlockchainDto): UnionAsset {
        return UnionAsset(
            type = convert(source.type, blockchain),
            value = source.value
        )
    }

    fun convert(source: com.rarible.protocol.solana.dto.AssetTypeDto, blockchain: BlockchainDto): UnionAssetType =
        when (source) {
            is com.rarible.protocol.solana.dto.SolanaNftAssetTypeDto -> UnionSolanaNftAssetType(
                itemId = ItemIdDto(blockchain, source.mint)
            )

            is com.rarible.protocol.solana.dto.SolanaFtAssetTypeDto -> UnionSolanaFtAssetType(
                address = ContractAddress(blockchain, source.mint)
            )

            is com.rarible.protocol.solana.dto.SolanaSolAssetTypeDto -> UnionSolanaSolAssetType()
        }

    @Deprecated("remove after migration to UnionOrder")
    fun convertLegacy(source: com.rarible.protocol.solana.dto.AssetDto, blockchain: BlockchainDto): AssetDto {
        return AssetDto(
            type = convertLegacy(source.type, blockchain),
            value = source.value
        )
    }

    @Deprecated("remove after migration to UnionOrder")
    fun convertLegacy(source: com.rarible.protocol.solana.dto.AssetTypeDto, blockchain: BlockchainDto): AssetTypeDto =
        when (source) {
            is com.rarible.protocol.solana.dto.SolanaNftAssetTypeDto -> SolanaNftAssetTypeDto(
                itemId = ItemIdDto(blockchain, source.mint)
            )

            is com.rarible.protocol.solana.dto.SolanaFtAssetTypeDto -> SolanaFtAssetTypeDto(
                address = ContractAddress(blockchain, source.mint)
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
