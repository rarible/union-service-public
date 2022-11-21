package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.data.annotation.Id

data class EsItemLite(
    @Id
    val id: String, // holds sha256 of itemId
    val itemId: String,
    val blockchain: BlockchainDto,
)