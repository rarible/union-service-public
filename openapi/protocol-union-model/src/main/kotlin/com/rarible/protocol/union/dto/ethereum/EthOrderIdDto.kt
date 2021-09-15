package com.rarible.protocol.union.dto.ethereum

import com.rarible.protocol.union.dto.EthBlockchainDto

data class EthOrderIdDto(
    override val blockchain: EthBlockchainDto,
    override val value: String
) : EthBlockchainId()