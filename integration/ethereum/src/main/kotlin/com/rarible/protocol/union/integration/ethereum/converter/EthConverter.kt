package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.protocol.dto.AmmNftAssetTypeDto
import com.rarible.protocol.dto.CollectionAssetTypeDto
import com.rarible.protocol.dto.CryptoPunksAssetTypeDto
import com.rarible.protocol.dto.Erc1155AssetTypeDto
import com.rarible.protocol.dto.Erc1155LazyAssetTypeDto
import com.rarible.protocol.dto.Erc20AssetTypeDto
import com.rarible.protocol.dto.Erc721AssetTypeDto
import com.rarible.protocol.dto.Erc721LazyAssetTypeDto
import com.rarible.protocol.dto.EthAssetTypeDto
import com.rarible.protocol.dto.GenerativeArtAssetTypeDto
import com.rarible.protocol.dto.NftItemRoyaltyDto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.exception.UnionValidationException
import com.rarible.protocol.union.core.model.UnionAssetDto
import com.rarible.protocol.union.core.model.UnionAssetTypeDto
import com.rarible.protocol.union.core.model.UnionEthAmmNftAssetTypeDto
import com.rarible.protocol.union.core.model.UnionEthCollectionAssetTypeDto
import com.rarible.protocol.union.core.model.UnionEthCryptoPunksAssetTypeDto
import com.rarible.protocol.union.core.model.UnionEthErc1155AssetTypeDto
import com.rarible.protocol.union.core.model.UnionEthErc1155LazyAssetTypeDto
import com.rarible.protocol.union.core.model.UnionEthErc20AssetTypeDto
import com.rarible.protocol.union.core.model.UnionEthErc721AssetTypeDto
import com.rarible.protocol.union.core.model.UnionEthErc721LazyAssetTypeDto
import com.rarible.protocol.union.core.model.UnionEthEthereumAssetTypeDto
import com.rarible.protocol.union.core.model.UnionEthGenerativeArtAssetTypeDto
import com.rarible.protocol.union.core.model.UnionEventTimeMarks
import com.rarible.protocol.union.core.model.UnionSourceEventTimeMark
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.AuctionBidDto
import com.rarible.protocol.union.dto.AuctionDataDto
import com.rarible.protocol.union.dto.AuctionSortDto
import com.rarible.protocol.union.dto.AuctionStatusDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.EthAmmNftAssetTypeDto
import com.rarible.protocol.union.dto.EthCollectionAssetTypeDto
import com.rarible.protocol.union.dto.EthCryptoPunksAssetTypeDto
import com.rarible.protocol.union.dto.EthErc1155AssetTypeDto
import com.rarible.protocol.union.dto.EthErc1155LazyAssetTypeDto
import com.rarible.protocol.union.dto.EthErc20AssetTypeDto
import com.rarible.protocol.union.dto.EthErc721AssetTypeDto
import com.rarible.protocol.union.dto.EthErc721LazyAssetTypeDto
import com.rarible.protocol.union.dto.EthEthereumAssetTypeDto
import com.rarible.protocol.union.dto.EthGenerativeArtAssetTypeDto
import com.rarible.protocol.union.dto.PayoutDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.RaribleAuctionV1BidDataV1Dto
import com.rarible.protocol.union.dto.RaribleAuctionV1BidV1Dto
import com.rarible.protocol.union.dto.RaribleAuctionV1DataV1Dto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.UnionAddress
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import scalether.domain.Address

object EthConverter {

    fun convert(address: Address): String = address.prefixed()!!
    fun convert(word: Word): String = word.prefixed()!!
    fun convert(binary: Binary): String = binary.prefixed()!!

    fun convertToWord(value: String): Word {
        try {
            return Word.apply(value)!!
        } catch (e: Throwable) {
            throw UnionValidationException("Incorrect Ethereum word format: $value")
        }
    }

    fun convertToAddress(value: String): Address {
        try {
            return Address.apply(value)!!
        } catch (e: Throwable) {
            throw UnionValidationException("Incorrect Ethereum address format: $value")
        }
    }

    fun convertToBinary(value: String): Binary {
        try {
            return Binary.apply(value)!!
        } catch (e: Throwable) {
            throw UnionValidationException("Incorrect Ethereum binary format: $value")
        }
    }

    fun convert(source: Address, blockchain: BlockchainDto): UnionAddress {
        return UnionAddressConverter.convert(blockchain, convert(source))
    }

    fun convert(source: SyncSortDto?): com.rarible.protocol.dto.SyncSortDto {
        return when (source) {
            null -> com.rarible.protocol.dto.SyncSortDto.DB_UPDATE_ASC
            SyncSortDto.DB_UPDATE_ASC -> com.rarible.protocol.dto.SyncSortDto.DB_UPDATE_ASC
            SyncSortDto.DB_UPDATE_DESC -> com.rarible.protocol.dto.SyncSortDto.DB_UPDATE_DESC
        }
    }

    fun convert(source: ActivitySortDto?): com.rarible.protocol.dto.ActivitySortDto {
        return when (source) {
            null -> com.rarible.protocol.dto.ActivitySortDto.LATEST_FIRST
            ActivitySortDto.EARLIEST_FIRST -> com.rarible.protocol.dto.ActivitySortDto.EARLIEST_FIRST
            ActivitySortDto.LATEST_FIRST -> com.rarible.protocol.dto.ActivitySortDto.LATEST_FIRST
        }
    }

    fun convert(source: AuctionSortDto?): com.rarible.protocol.dto.AuctionSortDto {
        return when (source) {
            null -> com.rarible.protocol.dto.AuctionSortDto.LAST_UPDATE_DESC
            AuctionSortDto.LAST_UPDATE_DESC -> com.rarible.protocol.dto.AuctionSortDto.LAST_UPDATE_DESC
            AuctionSortDto.LAST_UPDATE_ASC -> com.rarible.protocol.dto.AuctionSortDto.LAST_UPDATE_ASC
            AuctionSortDto.BUY_PRICE_ASC -> com.rarible.protocol.dto.AuctionSortDto.BUY_PRICE_ASC
        }
    }

    fun convert(source: PlatformDto?): com.rarible.protocol.dto.PlatformDto? {
        return when (source) {
            PlatformDto.RARIBLE, PlatformDto.IMMUTABLEX -> com.rarible.protocol.dto.PlatformDto.RARIBLE
            PlatformDto.OPEN_SEA -> com.rarible.protocol.dto.PlatformDto.OPEN_SEA
            PlatformDto.CRYPTO_PUNKS -> com.rarible.protocol.dto.PlatformDto.CRYPTO_PUNKS
            PlatformDto.X2Y2 -> com.rarible.protocol.dto.PlatformDto.X2Y2
            PlatformDto.LOOKSRARE -> com.rarible.protocol.dto.PlatformDto.LOOKSRARE
            PlatformDto.SUDOSWAP -> com.rarible.protocol.dto.PlatformDto.SUDOSWAP
            else -> null
        }
    }

    fun convertToPayout(source: PartDto, blockchain: BlockchainDto): PayoutDto {
        return PayoutDto(
            account = convert(source.account, blockchain),
            value = source.value
        )
    }

    fun convertToRoyalty(source: PartDto, blockchain: BlockchainDto): RoyaltyDto {
        return RoyaltyDto(
            account = convert(source.account, blockchain),
            value = source.value
        )
    }

    fun convertToCreator(source: PartDto, blockchain: BlockchainDto): CreatorDto {
        return CreatorDto(
            account = convert(source.account, blockchain),
            value = source.value
        )
    }

    @Deprecated("remove after migration to UnionOrder")
    fun convertLegacy(source: com.rarible.protocol.dto.AssetDto, blockchain: BlockchainDto): AssetDto {
        return AssetDto(
            type = convertLegacy(source.assetType, blockchain),
            value = source.valueDecimal!!
        )
    }

    fun convert(source: com.rarible.protocol.dto.AssetDto, blockchain: BlockchainDto): UnionAssetDto {
        return UnionAssetDto(
            type = convert(source.assetType, blockchain),
            value = source.valueDecimal!!
        )
    }

    fun convert(source: NftItemRoyaltyDto, blockchain: BlockchainDto): RoyaltyDto {
        return RoyaltyDto(
            account = UnionAddressConverter.convert(blockchain, convert(source.account)),
            value = source.value
        )
    }

    fun convert(source: com.rarible.protocol.dto.AssetTypeDto, blockchain: BlockchainDto): UnionAssetTypeDto {
        return when (source) {
            is EthAssetTypeDto -> UnionEthEthereumAssetTypeDto(blockchain)
            is Erc20AssetTypeDto -> UnionEthErc20AssetTypeDto(
                contract = ContractAddressConverter.convert(blockchain, convert(source.contract))
            )

            is Erc721AssetTypeDto -> UnionEthErc721AssetTypeDto(
                contract = ContractAddressConverter.convert(blockchain, convert(source.contract)),
                tokenId = source.tokenId
            )

            is Erc1155AssetTypeDto -> UnionEthErc1155AssetTypeDto(
                contract = ContractAddressConverter.convert(blockchain, convert(source.contract)),
                tokenId = source.tokenId
            )

            is Erc721LazyAssetTypeDto -> UnionEthErc721LazyAssetTypeDto(
                contract = ContractAddressConverter.convert(blockchain, convert(source.contract)),
                tokenId = source.tokenId,
                uri = source.uri,
                creators = source.creators.map { convertToCreator(it, blockchain) },
                royalties = source.royalties.map { convertToRoyalty(it, blockchain) },
                signatures = source.signatures.map { convert(it) }
            )

            is Erc1155LazyAssetTypeDto -> UnionEthErc1155LazyAssetTypeDto(
                contract = ContractAddressConverter.convert(blockchain, convert(source.contract)),
                tokenId = source.tokenId,
                uri = source.uri,
                supply = source.supply,
                creators = source.creators.map { convertToCreator(it, blockchain) },
                royalties = source.royalties.map { convertToRoyalty(it, blockchain) },
                signatures = source.signatures.map { convert(it) }
            )

            is CryptoPunksAssetTypeDto -> UnionEthCryptoPunksAssetTypeDto(
                contract = ContractAddressConverter.convert(blockchain, convert(source.contract)),
                tokenId = source.tokenId
            )

            is GenerativeArtAssetTypeDto -> UnionEthGenerativeArtAssetTypeDto(
                contract = ContractAddressConverter.convert(blockchain, convert(source.contract))
            )

            is CollectionAssetTypeDto -> UnionEthCollectionAssetTypeDto(
                contract = ContractAddressConverter.convert(blockchain, convert(source.contract))
            )

            is AmmNftAssetTypeDto -> UnionEthAmmNftAssetTypeDto(
                contract = ContractAddressConverter.convert(blockchain, convert(source.contract))
            )
        }
    }

    @Deprecated("remove after migration to UnionOrder")
    fun convertLegacy(source: com.rarible.protocol.dto.AssetTypeDto, blockchain: BlockchainDto): AssetTypeDto {
        return when (source) {
            is EthAssetTypeDto -> EthEthereumAssetTypeDto(blockchain)
            is Erc20AssetTypeDto -> EthErc20AssetTypeDto(
                contract = ContractAddressConverter.convert(blockchain, convert(source.contract))
            )

            is Erc721AssetTypeDto -> EthErc721AssetTypeDto(
                contract = ContractAddressConverter.convert(blockchain, convert(source.contract)),
                tokenId = source.tokenId
            )

            is Erc1155AssetTypeDto -> EthErc1155AssetTypeDto(
                contract = ContractAddressConverter.convert(blockchain, convert(source.contract)),
                tokenId = source.tokenId
            )

            is Erc721LazyAssetTypeDto -> EthErc721LazyAssetTypeDto(
                contract = ContractAddressConverter.convert(blockchain, convert(source.contract)),
                tokenId = source.tokenId,
                uri = source.uri,
                creators = source.creators.map { convertToCreator(it, blockchain) },
                royalties = source.royalties.map { convertToRoyalty(it, blockchain) },
                signatures = source.signatures.map { convert(it) }
            )

            is Erc1155LazyAssetTypeDto -> EthErc1155LazyAssetTypeDto(
                contract = ContractAddressConverter.convert(blockchain, convert(source.contract)),
                tokenId = source.tokenId,
                uri = source.uri,
                supply = source.supply,
                creators = source.creators.map { convertToCreator(it, blockchain) },
                royalties = source.royalties.map { convertToRoyalty(it, blockchain) },
                signatures = source.signatures.map { convert(it) }
            )

            is CryptoPunksAssetTypeDto -> EthCryptoPunksAssetTypeDto(
                contract = ContractAddressConverter.convert(blockchain, convert(source.contract)),
                tokenId = source.tokenId
            )

            is GenerativeArtAssetTypeDto -> EthGenerativeArtAssetTypeDto(
                contract = ContractAddressConverter.convert(blockchain, convert(source.contract))
            )

            is CollectionAssetTypeDto -> EthCollectionAssetTypeDto(
                contract = ContractAddressConverter.convert(blockchain, convert(source.contract))
            )

            is AmmNftAssetTypeDto -> EthAmmNftAssetTypeDto(
                contract = ContractAddressConverter.convert(blockchain, convert(source.contract))
            )
        }
    }

    fun convert(source: com.rarible.protocol.dto.AuctionBidDto, blockchain: BlockchainDto): AuctionBidDto {
        return when (source) {
            is com.rarible.protocol.dto.RaribleAuctionV1BidV1Dto -> RaribleAuctionV1BidV1Dto(
                buyer = convert(source.buyer, blockchain),
                amount = source.amount,
                data = RaribleAuctionV1BidDataV1Dto(
                    originFees = source.data.originFees.map { convertToPayout(it, blockchain) },
                    payouts = source.data.payouts.map { convertToPayout(it, blockchain) }
                ),
                date = source.date,
                status = convert(source.status)
            )
        }
    }

    fun convert(source: com.rarible.protocol.dto.AuctionDataDto, blockchain: BlockchainDto): AuctionDataDto {
        return when (source) {
            is com.rarible.protocol.dto.RaribleAuctionV1DataV1Dto -> RaribleAuctionV1DataV1Dto(
                originFees = source.originFees.map { convertToPayout(it, blockchain) },
                payouts = source.payouts.map { convertToPayout(it, blockchain) },
                startTime = source.startTime,
                duration = source.duration,
                buyOutPrice = source.buyOutPrice
            )
        }
    }

    fun convert(marks: com.rarible.protocol.dto.EventTimeMarksDto?): UnionEventTimeMarks? {
        marks ?: return null
        return UnionEventTimeMarks(
            source = marks.source,
            marks = marks.marks.map { UnionSourceEventTimeMark(it.name, it.date) }
        )
    }

    fun convert(source: List<AuctionStatusDto>?): List<com.rarible.protocol.dto.AuctionStatusDto>? {
        return source?.map { convert(it) }
    }

    private fun convert(source: AuctionStatusDto): com.rarible.protocol.dto.AuctionStatusDto {
        return when (source) {
            AuctionStatusDto.ACTIVE -> com.rarible.protocol.dto.AuctionStatusDto.ACTIVE
            AuctionStatusDto.CANCELLED -> com.rarible.protocol.dto.AuctionStatusDto.CANCELLED
            AuctionStatusDto.FINISHED -> com.rarible.protocol.dto.AuctionStatusDto.FINISHED
        }
    }

    private fun convert(source: com.rarible.protocol.dto.AuctionBidDto.Status): AuctionBidDto.Status {
        return when (source) {
            com.rarible.protocol.dto.AuctionBidDto.Status.ACTIVE -> AuctionBidDto.Status.ACTIVE
            com.rarible.protocol.dto.AuctionBidDto.Status.HISTORICAL -> AuctionBidDto.Status.HISTORICAL
        }
    }
}
