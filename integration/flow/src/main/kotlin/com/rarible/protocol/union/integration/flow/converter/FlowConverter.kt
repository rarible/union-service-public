package com.rarible.protocol.union.integration.flow.converter

import com.rarible.protocol.dto.FlowAssetDto
import com.rarible.protocol.dto.FlowAssetFungibleDto
import com.rarible.protocol.dto.FlowAssetNFTDto
import com.rarible.protocol.dto.FlowEventTimeMarksDto
import com.rarible.protocol.dto.PayInfoDto
import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionAsset
import com.rarible.protocol.union.core.model.UnionAssetType
import com.rarible.protocol.union.core.model.UnionEventTimeMarks
import com.rarible.protocol.union.core.model.UnionFlowAssetTypeFt
import com.rarible.protocol.union.core.model.UnionFlowAssetTypeNft
import com.rarible.protocol.union.core.model.UnionSourceEventTimeMark
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.FlowAssetTypeFtDto
import com.rarible.protocol.union.dto.FlowAssetTypeNftDto
import com.rarible.protocol.union.dto.PayoutDto
import java.math.BigDecimal

object FlowConverter {

    private val BP_MULTIPLIER = BigDecimal(10000)

    fun toBasePoints(v: BigDecimal): Int {
        return v.multiply(BP_MULTIPLIER).toInt()
    }

    fun convertToPayout(source: PayInfoDto, blockchain: BlockchainDto): PayoutDto {
        return PayoutDto(
            account = UnionAddressConverter.convert(blockchain, source.account),
            value = source.value.toInt()
        )
    }

    fun convertToCreator(source: PayInfoDto, blockchain: BlockchainDto): CreatorDto {
        return CreatorDto(
            account = UnionAddressConverter.convert(blockchain, source.account),
            value = toBasePoints(source.value)
        )
    }

    fun convert(source: FlowAssetDto, blockchain: BlockchainDto): UnionAsset {
        return when (source) {
            is FlowAssetFungibleDto -> {
                UnionAsset(
                    value = source.value,
                    type = UnionFlowAssetTypeFt(
                        contract = ContractAddressConverter.convert(blockchain, source.contract)
                    )
                )
            }

            is FlowAssetNFTDto -> {
                UnionAsset(
                    value = source.value,
                    type = UnionFlowAssetTypeNft(
                        contract = ContractAddressConverter.convert(blockchain, source.contract),
                        tokenId = source.tokenId
                    )
                )
            }
        }
    }

    fun convertToType(source: FlowAssetDto, blockchain: BlockchainDto): UnionAssetType {
        return when (source) {
            is FlowAssetFungibleDto -> {
                UnionFlowAssetTypeFt(
                    contract = ContractAddressConverter.convert(blockchain, source.contract)
                )
            }

            is FlowAssetNFTDto -> {
                UnionFlowAssetTypeNft(
                    contract = ContractAddressConverter.convert(blockchain, source.contract),
                    tokenId = source.tokenId
                )
            }
        }
    }

    @Deprecated("remove after migration to UnionOrder")
    fun convertLegacy(source: FlowAssetDto, blockchain: BlockchainDto): AssetDto {
        return when (source) {
            is FlowAssetFungibleDto -> {
                AssetDto(
                    value = source.value,
                    type = FlowAssetTypeFtDto(
                        contract = ContractAddressConverter.convert(blockchain, source.contract)
                    )
                )
            }

            is FlowAssetNFTDto -> {
                AssetDto(
                    value = source.value,
                    type = FlowAssetTypeNftDto(
                        contract = ContractAddressConverter.convert(blockchain, source.contract),
                        tokenId = source.tokenId,
                        collection = CollectionIdDto(blockchain, source.contract)
                    )
                )
            }
        }
    }

    @Deprecated("remove after migration to UnionOrder")
    fun convertToTypeLegacy(source: FlowAssetDto, blockchain: BlockchainDto): AssetTypeDto {
        return when (source) {
            is FlowAssetFungibleDto -> {
                FlowAssetTypeFtDto(
                    contract = ContractAddressConverter.convert(blockchain, source.contract)
                )
            }

            is FlowAssetNFTDto -> {
                FlowAssetTypeNftDto(
                    contract = ContractAddressConverter.convert(blockchain, source.contract),
                    tokenId = source.tokenId
                )
            }
        }
    }

    fun convert(marks: FlowEventTimeMarksDto?): UnionEventTimeMarks? {
        marks ?: return null
        return UnionEventTimeMarks(
            source = marks.source,
            marks = marks.marks.map { UnionSourceEventTimeMark(it.name, it.date) }
        )
    }

}
