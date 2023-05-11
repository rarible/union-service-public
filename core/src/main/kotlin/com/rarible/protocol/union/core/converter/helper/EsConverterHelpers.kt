package com.rarible.protocol.union.core.converter.helper

import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ext

fun getCurrencyAddressOrNull(blockchain: BlockchainDto, asset: AssetDto?): String? {
    return getCurrencyAddressOrNull(blockchain, asset?.type)
}

fun getCurrencyAddressOrNull(blockchain: BlockchainDto, assetType: AssetTypeDto?): String? {
    val address = runCatching {
        assetType?.ext?.currencyAddress()
    }.getOrNull() ?: return null
    return blockchain.name + ":" + address
}