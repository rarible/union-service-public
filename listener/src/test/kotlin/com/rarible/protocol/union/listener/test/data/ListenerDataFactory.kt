package com.rarible.protocol.union.listener.test.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.protocol.currency.dto.CurrencyRateDto
import com.rarible.protocol.union.listener.config.*
import java.util.*

fun defaultUnionListenerProperties(): UnionListenerProperties {
    return UnionListenerProperties(
        consumer = BlockchainConsumerSet(
            ethereum = ConsumerProperties(brokerReplicaSet = "KAFKA"),
            polygon = ConsumerProperties(brokerReplicaSet = "KAFKA"),
            flow = ConsumerProperties(brokerReplicaSet = "KAFKA")
        ),
        reconciliation = ReconciliationProperties(),
        priceUpdate = PriceUpdateProperties()
    )
}

fun createCurrencyDto(): CurrencyRateDto {
    return CurrencyRateDto(
        date = nowMillis(),
        fromCurrencyId = UUID.randomUUID().toString(),
        toCurrencyId = "usd",
        rate = randomBigDecimal()
    )
}
