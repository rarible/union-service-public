package com.rarible.protocol.union.api.model.internal

import com.rarible.protocol.union.dto.BlockchainDto

data class EsMaintenanceReindexTraitDto(
    val blockchains: List<BlockchainDto> = emptyList(),
    val collectionId: String? = null,
    val esIndex: String,
)
