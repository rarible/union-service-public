package com.rarible.protocol.union.enrichment.util

import com.rarible.protocol.union.dto.OrderDto

val OrderDto.sellCurrencyId: String
    get() = take.type.contract.value

val OrderDto.bidCurrencyId: String
    get() = make.type.contract.value
