package com.rarible.protocol.union.api.model.internal

import com.rarible.protocol.union.dto.BlockchainDto

data class EsMaintenanceReindexOwnershipDto(
    val blockchains: List<BlockchainDto> = emptyList(),
    val from: Long? = null,
    val to: Long? = null,
    val esIndex: String,
)
