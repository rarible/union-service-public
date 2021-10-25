package com.rarible.protocol.union.dto

class AssetTypeExtension(
    val isCurrency: Boolean,
    val isNft: Boolean,
    val contract: String,
    val itemId: ItemIdDto?
)