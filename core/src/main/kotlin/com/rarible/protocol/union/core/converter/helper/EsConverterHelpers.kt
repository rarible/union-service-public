package com.rarible.protocol.union.core.converter.helper

import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ext

fun getCurrencyIdOrNull(blockchain: BlockchainDto, asset: AssetDto?): String? {
    return getCurrencyIdOrNull(blockchain, asset?.type)
}

fun getCurrencyIdOrNull(blockchain: BlockchainDto, assetType: AssetTypeDto?): String? {
    val address = getCurrencyAddressOrNull(assetType) ?: return null
    return blockchain.name + ":" + address
}

fun getCurrencyAddressOrNull(assetType: AssetTypeDto?): String? {
    return runCatching {
        assetType?.ext?.currencyAddress()
    }.getOrNull()
}

fun getCurrencyAddressOrNull(asset: AssetDto?): String? {
    return getCurrencyAddressOrNull(asset?.type)
}