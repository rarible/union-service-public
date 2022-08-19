package com.rarible.protocol.union.integration.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.integration.immutablex.client.FeeToken
import com.rarible.protocol.union.integration.immutablex.client.FeeTokenData
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexAsset
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexCollectionShort
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexDataProperties
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexMint
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrder
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrderData
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrderFee
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrderSide
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTrade
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTransfer
import com.rarible.protocol.union.integration.immutablex.client.Token
import com.rarible.protocol.union.integration.immutablex.client.TokenData
import com.rarible.protocol.union.integration.immutablex.client.TradeSide
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.temporal.ChronoUnit

fun randomImxAsset(
    collection: ImmutablexCollectionShort = randomImxCollectionShort(),
    createdAt: Instant = nowMillis().minusSeconds(1),
    token: String = randomAddress().prefixed(),
    tokenId: String = randomBigInt(4).toString(),
    user: String = randomAddress().prefixed(),
    updatedAt: Instant = nowMillis()
): ImmutablexAsset {
    return ImmutablexAsset(
        collection = collection,
        createdAt = createdAt,
        description = "Description: " + randomString(),
        fees = emptyList(),
        id = tokenId,
        imageUrl = randomString(),
        metadata = mapOf("trait" to randomString()),
        name = "Name: " + randomString(),
        status = "imx",
        tokenAddress = token,
        tokenId = tokenId,
        uri = "http://localhost:8080/${randomString()}",
        updatedAt = updatedAt,
        user = user
    )
}

fun randomImxCollectionShort(): ImmutablexCollectionShort {
    return ImmutablexCollectionShort(
        iconUrl = "http://localhost:8080/${randomString()}",
        name = randomString()
    )
}

fun randomImxOrder(
    orderId: Long = randomLong(),
    buy: ImmutablexOrderSide = randomImxOrderBuySide(),
    sell: ImmutablexOrderSide = randomImxOrderSellSide(),
    status: String = "active",
    createdAt: Instant = nowMillis().minusSeconds(60),
    updatedAt: Instant = nowMillis().minusSeconds(30),
    creator: String = randomAddress().prefixed()
): ImmutablexOrder {
    return ImmutablexOrder(
        orderId = orderId,
        amountSold = "0",
        buy = buy,
        expirationTimestamp = nowMillis().plus(100, ChronoUnit.DAYS),
        fees = listOf(),
        sell = sell,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        creator = creator
    )
}

fun randomImxOrderSellSide(): ImmutablexOrderSide {
    return ImmutablexOrderSide(
        data = randomImxOrderSideData(),
        type = "ERC721"
    )
}

fun randomImxOrderBuySide(
    quantity: BigInteger = BigInteger("10000"),
    quantityWithFees: BigInteger = BigInteger("10000"),
    decimals: Int = 0,
    type: String = "ETH"
): ImmutablexOrderSide {
    return ImmutablexOrderSide(
        data = randomImxOrderSideData(
            decimals = decimals, quantity = quantity.toString(), quantityWithFees = quantityWithFees
        ),
        type = type
    )
}

fun randomImxOrderSideData(
    decimals: Int = 0,
    id: String = randomString(),
    token: String = randomAddress().prefixed(),
    tokenId: String = randomLong().toString(),
    quantity: String = "1",
    quantityWithFees: BigInteger = BigInteger("1")
): ImmutablexOrderData {
    return ImmutablexOrderData(
        decimals = decimals,
        id = id,
        quantity = quantity,
        quantityWithFees = quantityWithFees.toString(),
        tokenAddress = token,
        tokenId = tokenId,
        properties = null
    )
}

fun randomImxOrderFee(
    type: String = "royalty",
    address: String = randomAddress().prefixed(),
    amount: BigDecimal = randomBigDecimal(4, 2),
): ImmutablexOrderFee {
    return ImmutablexOrderFee(
        type = type,
        address = address,
        amount = amount,
        token = FeeToken("ETH", FeeTokenData(null, 18))
    )
}

fun randomImxTokenData(
    token: String = randomAddress().prefixed(),
    tokenId: String = randomLong().toString(),
): TokenData {
    return TokenData(
        tokenId = tokenId,
        tokenAddress = token,
        properties = ImmutablexDataProperties(
            name = randomString(),
            imageUrl = "http://localhost:8080/image/${randomString()}",
            collection = randomImxCollectionShort()
        ),
        decimals = 0,
        quantity = BigInteger.ONE,
        id = tokenId
    )
}

fun randomImxMint(
    transactionId: Long = randomLong(),
    token: String = randomAddress().prefixed(),
    tokenId: String = randomLong().toString(),
    user: String = randomAddress().prefixed(),
    date: Instant = nowMillis()
): ImmutablexMint {
    return ImmutablexMint(
        transactionId = transactionId,
        token = Token(
            type = "ERC721",
            data = randomImxTokenData(token, tokenId)
        ),

        user = user,
        timestamp = date,
        fees = listOf(),
        status = "success"
    )
}

fun randomImxTransfer(
    transactionId: Long = randomLong(),
    token: String = randomAddress().prefixed(),
    tokenId: String = randomLong().toString(),
    user: String = randomAddress().prefixed(),
    receiver: String = randomAddress().prefixed(),
    date: Instant = nowMillis()
): ImmutablexTransfer {
    return ImmutablexTransfer(
        transactionId = transactionId,
        token = Token(
            type = "ERC721",
            data = randomImxTokenData(token, tokenId)
        ),
        receiver = receiver,
        status = "success",
        timestamp = date,
        user = user
    )
}

fun randomImxTrade(
    transactionId: Long = randomLong(),
    date: Instant = nowMillis(),
    sellOrderId: Long = randomLong(),
    buyOrderId: Long = randomLong(),
    sellToken: String = randomAddress().prefixed(),
    sellTokenId: String = randomLong().toString(),
    buyToken: String = randomAddress().prefixed(),
    buyTokenId: String = randomLong().toString(),
): ImmutablexTrade {
    return ImmutablexTrade(
        transactionId = transactionId,
        make = randomImxTradeSide(
            orderId = sellOrderId,
            token = sellToken,
            tokenId = sellTokenId
        ),
        take = randomImxTradeSide(
            orderId = buyOrderId,
            token = buyToken,
            tokenId = buyTokenId
        ),
        status = "success",
        timestamp = date
    )
}

fun randomImxTradeSide(
    orderId: Long = randomLong(),
    token: String? = randomAddress().prefixed(),
    tokenId: String? = randomLong().toString(),
    tokenType: String = "ERC721"
): TradeSide {
    return TradeSide(
        orderId = orderId,
        sold = BigDecimal.ONE,
        tokenAddress = token,
        tokenId = tokenId,
        tokenType = tokenType
    )
}