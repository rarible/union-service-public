package com.rarible.protocol.union.listener.tezos

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.dipdup.client.core.model.Asset
import com.rarible.dipdup.client.core.model.DipDupActivity
import com.rarible.dipdup.client.core.model.DipDupBurnActivity
import com.rarible.dipdup.client.core.model.DipDupMintActivity
import com.rarible.dipdup.client.core.model.DipDupOrderListActivity
import com.rarible.dipdup.client.core.model.DipDupTransferActivity
import com.rarible.dipdup.client.core.model.TezosPlatform
import com.rarible.tzkt.model.Alias
import com.rarible.tzkt.model.Token
import com.rarible.tzkt.model.TokenBalance
import com.rarible.tzkt.model.TokenInfo
import java.math.BigDecimal
import java.math.BigInteger
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

fun randomDipDupActivityOrderListEvent(activityId: String): DipDupActivity {
    return DipDupOrderListActivity(
        id = activityId,
        orderId = "0",
        date = nowMillis().atOffset(ZoneOffset.UTC),
        reverted = false,
        hash = "",
        maker = UUID.randomUUID().toString(),
        make = Asset(
            assetType = Asset.NFT(
                contract = UUID.randomUUID().toString(),
                tokenId = BigInteger.ONE
            ),
            assetValue = BigDecimal.ONE
        ),
        take = Asset(
            assetType = Asset.XTZ(),
            assetValue = BigDecimal.ONE
        ),
        operationCounter = randomInt(),
        source = TezosPlatform.RARIBLE_V2,
        dbUpdatedAt = null
    )
}

fun randomDipDupActivityMintEvent(): DipDupMintActivity {
    return DipDupMintActivity(
        id = randomString(),
        transferId = randomString(),
        contract = randomString(),
        tokenId = randomBigInt(),
        value = BigDecimal("1"),
        owner = randomString(),
        date = OffsetDateTime.now(),
        reverted = false,
        transactionId = randomString(),
        dbUpdatedAt = null
    )
}

fun randomDipDupActivityTransferEvent(): DipDupTransferActivity {
    return DipDupTransferActivity(
        id = randomString(),
        transferId = randomString(),
        contract = randomString(),
        tokenId = randomBigInt(),
        value = BigDecimal("1"),
        owner = randomString(),
        from = randomString(),
        date = OffsetDateTime.now(),
        reverted = false,
        transactionId = randomString(),
        dbUpdatedAt = null
    )
}

fun randomDipDupActivityBurnEvent(): DipDupBurnActivity {
    return DipDupBurnActivity(
        id = randomString(),
        transferId = randomString(),
        contract = randomString(),
        tokenId = randomBigInt(),
        value = BigDecimal("1"),
        owner = randomString(),
        date = OffsetDateTime.now(),
        reverted = false,
        transactionId = randomString(),
        dbUpdatedAt = null
    )
}

fun randomTzktTokenBalance(contract: String, tokenId: BigInteger, owner: String): TokenBalance {
    return TokenBalance(
        id = 1,
        account = Alias(
            address = owner,
        ),
        token = TokenInfo(
            contract = Alias(
                address = contract
            ),
            tokenId = tokenId.toString()
        ),
        balance = "1",
        firstLevel = 1,
        firstTime = OffsetDateTime.now(),
        lastLevel = 1,
        lastTime = OffsetDateTime.now(),
        transfersCount = 1
    )
}

fun randomTzktToken() =
    randomTzktToken(randomString(), randomBigInt(), BigInteger.ONE)

fun randomTzktToken(contract: String, tokenId: BigInteger, supply: BigInteger): Token {
    return Token(
        id = randomLong(),
        contract = Alias(
            address = contract
        ),
        tokenId = tokenId.toString(),
        balancesCount = 1,
        holdersCount = 1,
        transfersCount = 1,
        metadata = mapOf("artifactUri" to Object()),
        totalSupply = supply.toString(),
        firstTime = OffsetDateTime.now(),
        lastTime = OffsetDateTime.now()
    )
}
