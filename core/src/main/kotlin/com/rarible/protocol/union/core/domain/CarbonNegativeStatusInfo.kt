package com.rarible.domain

import com.rarible.marketplace.core.model.BlockchainAddress
import java.util.Date

data class CarbonNegativeStatusInfo(
    val sponsor: BlockchainAddress,
    val paymentDate: Date
)
