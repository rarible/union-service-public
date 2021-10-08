package com.rarible.protocol.union.enrichment.model

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderIdDto
import java.math.BigDecimal
import java.math.BigInteger

data class ShortOrder(

    val blockchain: BlockchainDto,
    val id: String,

    val platform: String,
    val makeStock: BigInteger,

    val makePrice: BigDecimal?,
    val takePrice: BigDecimal?,

    @Transient
    val makePriceUsd: BigDecimal?,
    @Transient
    val takePriceUsd: BigDecimal?,

    @Transient
    val dtoId: OrderIdDto = OrderIdDto(blockchain, id)

) {
    fun getIdDto(): OrderIdDto {
        return OrderIdDto(blockchain, id)
    }

    fun clearState(): ShortOrder {
        return copy(makePriceUsd = null, takePriceUsd = null)
    }
}
