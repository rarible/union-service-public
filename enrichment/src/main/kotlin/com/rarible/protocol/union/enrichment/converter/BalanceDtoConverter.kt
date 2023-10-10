package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.model.UnionBalance
import com.rarible.protocol.union.dto.BalanceDto

object BalanceDtoConverter {

    fun convert(source: UnionBalance): BalanceDto {
        return BalanceDto(
            currencyId = source.currencyId,
            owner = source.owner,
            balance = source.balance,
            decimal = source.decimal
        )
    }
}
