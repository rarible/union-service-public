package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.ItemIdDto

class AssetTypeExtension(
    val isCurrency: Boolean,
    val isNft: Boolean,
    val contract: String,
    val itemId: ItemIdDto?
)