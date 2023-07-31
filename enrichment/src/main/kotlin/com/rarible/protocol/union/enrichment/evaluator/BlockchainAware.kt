package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.dto.BlockchainDto

interface BlockchainAware {
    val blockchain: BlockchainDto
}
