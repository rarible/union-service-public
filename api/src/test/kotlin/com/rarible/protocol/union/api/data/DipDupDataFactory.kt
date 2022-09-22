package com.rarible.protocol.union.api.data

import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.dipdup.client.core.model.Asset
import com.rarible.dipdup.client.core.model.DipDupActivity
import com.rarible.dipdup.client.core.model.DipDupOrderListActivity
import com.rarible.dipdup.client.core.model.TezosPlatform
import com.rarible.tzkt.model.ActivityType
import com.rarible.tzkt.model.Alias
import com.rarible.tzkt.model.TokenActivity
import com.rarible.tzkt.model.TokenInfo
import com.rarible.tzkt.model.TypedTokenActivity
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

fun randomDipDupListActivity(activityId: String, date: Instant): DipDupActivity {
    return DipDupOrderListActivity(
        id = activityId,
        operationCounter = randomInt(),
        date = date.atOffset(ZoneOffset.UTC),
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
        source = TezosPlatform.RARIBLE_V2
    )
}

fun randomTzktItemMintActivity(activityId: String, date: Instant): TypedTokenActivity {
    return TypedTokenActivity(
        type = ActivityType.MINT,
        tokenActivity = TokenActivity(
            id = activityId.toLong(),
            token = TokenInfo(
                id = 1,
                contract = Alias(
                    address = randomString()
                ),
                tokenId = "1"
            ),
            level = 1,
            timestamp = date.atOffset(ZoneOffset.UTC),
            to = Alias(
                address = randomString()
            ),
            amount = "1",
            transactionId = randomLong()
        )
    )
}
