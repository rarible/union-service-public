package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.UnionAddress

data class UnionOrderActivityMatchSideDto(
    val maker: UnionAddress,
    val hash: String? = null,
    val asset: UnionAssetDto
)