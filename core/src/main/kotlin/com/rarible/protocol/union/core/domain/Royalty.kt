package com.rarible.protocol.union.core.domain

import com.rarible.marketplace.core.model.BlockchainAddress

data class Royalty(
    val recipient: BlockchainAddress,
    val value: Long
)
