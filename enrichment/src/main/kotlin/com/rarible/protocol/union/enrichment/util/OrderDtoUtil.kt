package com.rarible.protocol.union.enrichment.util

import com.rarible.protocol.union.core.model.ext
import com.rarible.protocol.union.dto.OrderDto

val OrderDto.sellCurrencyId: String
    get() = take.type.ext.contract

val OrderDto.bidCurrencyId: String
    get() = make.type.ext.contract
