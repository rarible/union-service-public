package com.rarible.protocol.union.test.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.*
import com.rarible.protocol.dto.*
import com.rarible.protocol.union.core.flow.converter.FlowAddressConverter
import com.rarible.protocol.union.dto.FlowAssetTypeFtDto
import com.rarible.protocol.union.dto.FlowBlockchainDto
import com.rarible.protocol.union.dto.FlowItemIdDto
import com.rarible.protocol.union.dto.FlowOwnershipIdDto
import com.rarible.protocol.union.dto.flow.FlowAddress
import com.rarible.protocol.union.dto.flow.FlowContract
import com.rarible.protocol.union.dto.flow.FlowItemIdProvider
import com.rarible.protocol.union.dto.flow.FlowOwnershipIdProvider
import java.math.BigInteger

fun randomFlowContract() = FlowContract(FlowBlockchainDto.FLOW, randomString(12))
fun randomFlowAddress() = FlowAddress(FlowBlockchainDto.FLOW, randomString(16))

fun randomFlowItemIdShortValue() = "${randomFlowContract().value}:${randomLong()}"
fun randomFlowItemIdFullValue() = "FLOW:${randomFlowItemIdShortValue()}"

fun randomFlowItemId() = FlowItemIdProvider.parseFull(randomFlowItemIdFullValue())

fun randomFlowOwnershipIdShortValue() = "${randomFlowItemIdShortValue()}:${randomFlowAddress().value}"
fun randomFlowOwnershipIdFullValue() = "FLOW:${randomFlowOwnershipIdShortValue()}"

fun randomFlowOwnershipId() = FlowOwnershipIdProvider.parseFull(randomFlowOwnershipIdFullValue())
fun randomFlowOwnershipId(itemId: FlowItemIdDto) = randomFlowOwnershipId(itemId, randomFlowAddress().value)

fun randomFlowOwnershipId(itemId: FlowItemIdDto, owner: String): FlowOwnershipIdDto {
    return FlowOwnershipIdDto(
        value = "${itemId.value}:${owner}",
        token = FlowContract(FlowBlockchainDto.FLOW, itemId.token.value),
        tokenId = itemId.tokenId,
        owner = FlowAddress(FlowBlockchainDto.FLOW, owner),
        blockchain = FlowBlockchainDto.FLOW
    )
}

fun randomFlowNftItemDto() = randomFlowNftItemDto(randomFlowItemId(), randomString())
fun randomFlowNftItemDto(itemId: FlowItemIdDto) = randomFlowNftItemDto(itemId, randomString())
fun randomFlowNftItemDto(itemId: FlowItemIdDto, creator: String): FlowNftItemDto {
    return FlowNftItemDto(
        id = itemId.value,
        collection = itemId.token.value,
        tokenId = itemId.tokenId,
        mintedAt = nowMillis(),
        lastUpdatedAt = nowMillis(),
        meta = null,
        creators = listOf(FlowCreatorDto(creator, randomBigDecimal())),
        owners = emptyList(),
        royalties = emptyList(),
        metaUrl = randomString(),
        supply = randomBigInt(),
        deleted = randomBoolean()
    )
}

fun randomFlowNftOwnershipDto() = randomFlowNftOwnershipDto(randomFlowOwnershipId())
fun randomFlowNftOwnershipDto(ownershipId: FlowOwnershipIdDto) = randomFlowNftOwnershipDto(
    FlowItemIdProvider.parseShort("${ownershipId.token.value}:${ownershipId.tokenId}", FlowBlockchainDto.FLOW),
    randomString()
)

fun randomFlowNftOwnershipDto(itemId: FlowItemIdDto, creator: String): FlowNftOwnershipDto {
    val ownershipId = randomFlowOwnershipId(itemId, creator)
    return FlowNftOwnershipDto(
        id = ownershipId.value,
        contract = ownershipId.token.value,
        tokenId = ownershipId.tokenId.toLong(),
        owner = ownershipId.owner.value,
        creators = listOf(PayInfoDto(creator, randomBigDecimal())),
        createdAt = nowMillis()
    )
}

/*
    public final val amount: java.math.BigDecimal /* compiled code */

    public final val amountUsd: java.math.BigDecimal /* compiled code */

    public final val cancelled: kotlin.Boolean /* compiled code */

    public final val collection: kotlin.String /* compiled code */

    public final val createdAt: java.time.Instant /* compiled code */

    public final val data: com.rarible.protocol.dto.FlowOrderDataDto /* compiled code */

    public final val fill: java.math.BigInteger /* compiled code */

    public final val itemId: kotlin.String /* compiled code */

    public final val lastUpdateAt: java.time.Instant /* compiled code */

    public final val make: com.rarible.protocol.dto.FlowAssetDto /* compiled code */

    public final val maker: kotlin.String /* compiled code */

    public final val offeredNftId: kotlin.String? /* compiled code */

    public final val take: com.rarible.protocol.dto.FlowAssetDto? /* compiled code */

    public final val taker: kotlin.String? /* compiled code */
 */

fun randomFlowV1Order(): FlowOrderDto {
    return FlowOrderDto(
        itemId = randomFlowItemIdShortValue(),
        maker = randomFlowAddress().value,
        taker = randomFlowAddress().value,
        make = randomFlowAsset(),
        take = randomFlowAsset(),
        fill = randomBigInt(),
        cancelled = randomBoolean(),
        createdAt = nowMillis(),
        amount = randomBigDecimal(),
        amountUsd = randomBigDecimal(),
        data = FlowOrderDataDto(
            payouts = listOf(),
            originalFees = listOf()
        ),
        collection = randomFlowContract().value,
        lastUpdateAt = nowMillis(),
        offeredNftId = randomString()
    )
}

fun randomFlowAsset(): FlowAssetDto {
    return randomFlowNftAssetType()
}


fun randomFlowFungibleAssetType() = randomFlowFungibleAssetType(randomFlowAddress())
fun randomFlowFungibleAssetType(contract: FlowAddress) = FlowAssetTypeFtDto(
    contract = FlowAddressConverter.convert(contract.value, FlowBlockchainDto.FLOW)
)

fun randomFlowNftAssetType() = randomFlowNftAssetType(randomFlowAddress(), randomBigInt())
fun randomFlowNftAssetType(contract: FlowAddress, tokenId: BigInteger) = FlowAssetNFTDto(
    contract = contract.value,
    tokenId = tokenId,
    value = randomBigDecimal()
)