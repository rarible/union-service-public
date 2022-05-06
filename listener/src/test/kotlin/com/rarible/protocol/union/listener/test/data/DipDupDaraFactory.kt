package com.rarible.protocol.union.listener.tezos

import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomInt
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
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*


fun randomDipDupActivityOrderListEvent(activityId: String): DipDupActivity {
    return DipDupOrderListActivity(
        id = activityId,
        date = Instant.now().atOffset(ZoneOffset.UTC),
        reverted = false,
        hash = "",
        maker = UUID.randomUUID().toString(),
        make = Asset(
            type = Asset.NFT(
                contract = UUID.randomUUID().toString(),
                tokenId = BigInteger.ONE
            ),
            value = BigDecimal.ONE
        ),
        take = Asset(
            type = Asset.XTZ(),
            value = BigDecimal.ONE
        ),
        price = BigDecimal.ONE,
        source = TezosPlatform.RARIBLE
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

fun randomTzktToken(contract: String, tokenId: BigInteger, supply: BigInteger): Token {
    return Token(
        id = randomInt(),
        contract = Alias(
            address = contract
        ),
        tokenId = tokenId.toString(),
        balancesCount = 1,
        holdersCount = 1,
        transfersCount = 1,
        totalSupply = supply.toString(),
        firstTime = OffsetDateTime.now(),
        lastTime = OffsetDateTime.now()
    )
}
