package com.rarible.protocol.union.dto.flow

import com.rarible.protocol.union.dto.FlowBlockchainDto

data class FlowAddress(
    override val blockchain: FlowBlockchainDto,
    override val value: String
) : FlowBlockchainId()