package com.rarible.protocol.union.search.test

import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.search.core.ElasticActivity
import java.time.Instant

fun buildActivity() = ElasticActivity(
    activityId = randomString(),
    date = Instant.now(),
    blockNumber = randomLong(),
    logIndex = randomInt(),
    blockchain = BlockchainDto.values().random(),
    type = ActivityTypeDto.values().random(),
    user = ElasticActivity.User(
        maker = randomString(),
        taker = randomString(),
    ),
    collection = ElasticActivity.Collection(
        make = randomString(),
        take = randomString(),
    ),
    item = ElasticActivity.Item(
        make = randomString(),
        take = randomString(),
    ),
)
