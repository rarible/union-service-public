package com.rarible.protocol.union.enrichment.model

import com.rarible.protocol.union.dto.*

data class CurrencyId(
    val blockchain: BlockchainDto,
    val address: String,
    val type: CurrencyType
)
