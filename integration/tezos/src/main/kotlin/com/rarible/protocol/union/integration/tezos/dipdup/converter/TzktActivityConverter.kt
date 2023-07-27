package com.rarible.protocol.union.integration.tezos.dipdup.converter

import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.exception.UnionDataFormatException
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.model.UnionBurnActivity
import com.rarible.protocol.union.core.model.UnionMintActivity
import com.rarible.protocol.union.core.model.UnionTransferActivity
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.tzkt.model.ActivityType
import com.rarible.tzkt.model.TypedTokenActivity
import java.math.BigDecimal
import java.math.BigInteger

object TzktActivityConverter {

    fun convertToTzktTypes(source: List<ActivityTypeDto>): List<ActivityType> {
        return source.mapNotNull { convertToTzktType(it) }
    }

    fun convertToTzktType(source: ActivityTypeDto) = when (source) {
        ActivityTypeDto.MINT -> ActivityType.MINT
        ActivityTypeDto.TRANSFER -> ActivityType.TRANSFER
        ActivityTypeDto.BURN -> ActivityType.BURN
        else -> null
    }

    fun convert(source: TypedTokenActivity, blockchain: BlockchainDto): UnionActivity {
        val id = ActivityIdDto(blockchain, source.id.toString())
        return when (source.type) {
            ActivityType.MINT -> UnionMintActivity(
                id = id,
                date = source.timestamp.toInstant(),
                reverted = false,
                owner = UnionAddressConverter.convert(blockchain, source.to!!.address),
                transactionHash = source.transactionHash.toString(),
                value = convertValue(BigDecimal(source.amount), id),
                tokenId = source.tokenId(),
                itemId = ItemIdDto(blockchain, source.contract(), source.tokenId()),
                contract = ContractAddress(blockchain, source.contract()),
                collection = CollectionIdDto(blockchain, source.contract())
            )

            ActivityType.BURN -> UnionBurnActivity(
                id = id,
                date = source.timestamp.toInstant(),
                reverted = false,
                owner = UnionAddressConverter.convert(blockchain, source.from!!.address),
                transactionHash = source.transactionHash.toString(),
                value = convertValue(BigDecimal(source.amount), id),
                tokenId = source.tokenId(),
                itemId = ItemIdDto(blockchain, source.contract(), source.tokenId()),
                contract = ContractAddress(blockchain, source.contract()),
                collection = CollectionIdDto(blockchain, source.contract())
            )

            ActivityType.TRANSFER -> UnionTransferActivity(
                id = id,
                date = source.timestamp.toInstant(),
                reverted = false,
                from = UnionAddressConverter.convert(blockchain, source.from!!.address),
                owner = UnionAddressConverter.convert(blockchain, source.to!!.address),
                transactionHash = source.transactionHash.toString(),
                value = convertValue(BigDecimal(source.amount), id),
                tokenId = source.tokenId(),
                itemId = ItemIdDto(blockchain, source.contract(), source.tokenId()),
                contract = ContractAddress(blockchain, source.contract()),
                collection = CollectionIdDto(blockchain, source.contract())
            )
        }
    }

    private fun convertValue(bd: BigDecimal, id: ActivityIdDto): BigInteger {
        if (bd.stripTrailingZeros()
            .scale() > 0
        ) throw UnionDataFormatException("Value: $bd must be BigInteger for token activity: $id")
        else return bd.toBigInteger()
    }

    private fun TypedTokenActivity.tokenId(): BigInteger {
        return this.token?.tokenId?.toBigInteger()
            ?: throw UnionDataFormatException("Token must have tokenId for activity: $id")
    }

    private fun TypedTokenActivity.contract(): String {
        return this.token?.contract?.address
            ?: throw UnionDataFormatException("Token must have address for activity: $id")
    }
}
