package com.rarible.protocol.union.core.domain

import com.rarible.marketplace.core.model.BlockchainAddress
import org.bson.types.ObjectId
import org.springframework.data.annotation.Transient
import java.math.BigDecimal
import java.util.Date

data class Auction(
    val id: String = ObjectId.get().toHexString(),
    val minPrice: BigDecimal,
    val minStep: BigDecimal = BigDecimal.ZERO,
    val currency: BlockchainAddress,
    val currencyTokenId: String? = null,
    val startDate: Date,
    val initEndDate: Date,
    val endDate: Date = initEndDate,
    val updateDate: Date = Date(),
    val status: AuctionStatus = AuctionStatus.NOT_STARTED,
    val onChain: Boolean = false,
    val buyOutPrice: BigDecimal? = null
) {
    @get:Transient
    val isExtended: Boolean
        get() = endDate != initEndDate

    @get:Transient
    val isInProgress: Boolean
        get() = containsDate(Date())

    fun containsDate(date: Date): Boolean = date.after(startDate) && date.before(endDate)
}

enum class AuctionStatus {
    NOT_STARTED,
    STARTED,
    ENDED
}
