package com.rarible.protocol.union.enrichment.util

import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.enrichment.converter.CurrencyIdConverter
import com.rarible.protocol.union.enrichment.model.CurrencyId

val OrderDto.sellCurrencyId: CurrencyId
    get() = CurrencyIdConverter.convert(take.type)

val OrderDto.bidCurrencyId: CurrencyId
    get() = CurrencyIdConverter.convert(make.type)
