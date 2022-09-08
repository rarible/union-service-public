package com.rarible.protocol.union.core.converter.helper

import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ext

fun getCurrencyAddressOrNull(blockchain: BlockchainDto, asset: AssetDto?): String? {
    val address = runCatching {
        asset?.type?.ext?.currencyAddress()
    }.getOrNull() ?: return null
    return blockchain.name + ":" + address
}