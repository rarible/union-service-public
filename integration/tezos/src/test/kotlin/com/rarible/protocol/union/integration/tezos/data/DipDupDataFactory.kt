package com.rarible.protocol.union.integration.tezos.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.dipdup.client.core.model.Asset
import com.rarible.dipdup.client.core.model.DipDupActivity
import com.rarible.dipdup.client.core.model.DipDupCollection
import com.rarible.dipdup.client.core.model.DipDupMintActivity
import com.rarible.dipdup.client.core.model.DipDupOrderListActivity
import com.rarible.dipdup.client.core.model.TezosPlatform
import com.rarible.dipdup.listener.model.DipDupCollectionEvent
import com.rarible.tzkt.model.ActivityType
import com.rarible.tzkt.model.Alias
import com.rarible.tzkt.model.TokenActivity
import com.rarible.tzkt.model.TokenInfo
import com.rarible.tzkt.model.TypedTokenActivity
import java.math.BigDecimal
import java.math.BigInteger
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

fun randomDipDupActivityOrderListEvent(activityId: String): DipDupActivity {
    return DipDupOrderListActivity(
        id = activityId,
        orderId = "0",
        operationCounter = randomInt(),
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
        source = TezosPlatform.RARIBLE_V2,
        dbUpdatedAt = null
    )
}

fun randomDipDupActivityMint(activityId: String): DipDupMintActivity {
    return DipDupMintActivity(
        id = activityId,
        date = nowMillis().atOffset(ZoneOffset.UTC),
        reverted = false,
        transferId = randomLong().toString(),
        contract = UUID.randomUUID().toString(),
        tokenId = BigInteger.ONE,
        value = BigDecimal.ONE,
        transactionId = randomLong().toString(),
        owner = UUID.randomUUID().toString(),
        dbUpdatedAt = null
    )
}

fun randomTzktItemMintActivity(activityId: String): TypedTokenActivity {
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
            timestamp = OffsetDateTime.now(),
            to = Alias(
                address = randomString()
            ),
            amount = "1",
            transactionId = randomLong()
        )
    )
}

fun randomTzktItemBurnActivity(activityId: String): TypedTokenActivity {
    return TypedTokenActivity(
        type = ActivityType.BURN,
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
            timestamp = OffsetDateTime.now(),
            from = Alias(
                address = randomString()
            ),
            amount = "1",
            transactionId = randomLong()
        )
    )
}

fun randomDipDupCollectionEvent(collectionId: String): DipDupCollectionEvent {
    return DipDupCollectionEvent(
        id = UUID.randomUUID(),
        eventId = randomString(),
        collection = DipDupCollection(
            id = collectionId,
            owner = randomString(),
            name = randomString(),
            minters = emptyList(),
            standard = "fa2",
            symbol = null
        ),
        type = "UPDATE"
    )
}
