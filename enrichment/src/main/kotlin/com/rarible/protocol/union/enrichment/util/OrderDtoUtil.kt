package com.rarible.protocol.union.enrichment.util

import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.ext

val OrderDto.sellCurrencyId: String
    get() = take.type.ext.contract

val OrderDto.bidCurrencyId: String
    get() = make.type.ext.contract
