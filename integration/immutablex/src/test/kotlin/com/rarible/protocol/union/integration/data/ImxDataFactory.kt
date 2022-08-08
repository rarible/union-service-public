package com.rarible.protocol.union.integration.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.integration.immutablex.dto.FeeToken
import com.rarible.protocol.union.integration.immutablex.dto.FeeTokenData
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexOrder
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexOrderData
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexOrderFee
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexOrderSide
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.temporal.ChronoUnit

fun randomImxOrder(
    orderId: Long = randomLong(),
    amountSold: String = "0",
    buy: ImmutablexOrderSide = randomEmxOrderBuySide(),
    sell: ImmutablexOrderSide = randomEmxOrderSellSide(),
    status: String = "active",
    createdAt: Instant = nowMillis().minusSeconds(60),
    updatedAt: Instant = nowMillis().minusSeconds(30),
    creator: String = randomAddress().prefixed()
): ImmutablexOrder {
    return ImmutablexOrder(
        orderId = orderId,
        amountSold = amountSold,
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

fun randomEmxOrderSellSide(): ImmutablexOrderSide {
    return ImmutablexOrderSide(
        data = randomImxOrderSideData(),
        type = "ERC721"
    )
}

fun randomEmxOrderBuySide(
    quantity: BigInteger = BigInteger("10000")
): ImmutablexOrderSide {
    return ImmutablexOrderSide(
        data = randomImxOrderSideData(decimals = 0, quantity = quantity),
        type = "ETH"
    )
}

fun randomImxOrderSideData(
    decimals: Int = 0,
    id: String = randomString(),
    token: String = randomAddress().prefixed(),
    tokenId: String = randomLong().toString(),
    quantity: BigInteger = BigInteger("1")
): ImmutablexOrderData {
    return ImmutablexOrderData(
        decimals = decimals,
        id = id,
        quantity = quantity,
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