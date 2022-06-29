package com.rarible.protocol.union.integration.tezos.data

import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomString
import com.rarible.dipdup.client.core.model.Asset
import com.rarible.dipdup.client.core.model.DipDupActivity
import com.rarible.dipdup.client.core.model.DipDupCollection
import com.rarible.dipdup.client.core.model.DipDupOrderListActivity
import com.rarible.dipdup.client.core.model.EventType
import com.rarible.dipdup.client.core.model.TezosPlatform
import com.rarible.tzkt.model.ActivityType
import com.rarible.tzkt.model.Alias
import com.rarible.tzkt.model.TokenActivity
import com.rarible.tzkt.model.TokenInfo
import com.rarible.tzkt.model.TypedTokenActivity
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
        source = TezosPlatform.Rarible
    )
}

fun randomTzktItemMintActivity(activityId: String): TypedTokenActivity {
    return TypedTokenActivity(
        type = ActivityType.MINT,
        tokenActivity = TokenActivity(
            id = activityId.toInt(),
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
            transactionId = randomInt()
        )
    )
}

fun randomDipDupCollectionEvent(collectionId: String): DipDupCollection {
    return DipDupCollection(
        id = UUID.randomUUID(),
        network = "test",
        eventId = randomString(),
        collection = DipDupCollection.Collection(
            id = collectionId,
            owner = randomString(),
            name = randomString(),
            minters = emptyList(),
            standard = "fa2",
            symbol = null
        ),
        type = EventType.UPDATE
    )
}
