package com.rarible.protocol.union.core.util

import com.rarible.protocol.union.core.model.UnionAsset
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.ext
import java.math.BigDecimal

@Deprecated("keep only UnionAsset")
fun evalMakePrice(make: AssetDto, take: AssetDto): BigDecimal? {
    return if (make.type.ext.isNft) take.value.setScale(18) / make.value else null
}

@Deprecated("keep only UnionAsset")
fun evalTakePrice(make: AssetDto, take: AssetDto): BigDecimal? {
    return if (take.type.ext.isNft) make.value.setScale(18) / take.value else null
}

fun evalMakePrice(make: UnionAsset, take: UnionAsset): BigDecimal? {
    return if (make.type.isNft()) take.value.setScale(18) / make.value else null
}

fun evalTakePrice(make: UnionAsset, take: UnionAsset): BigDecimal? {
    return if (take.type.isNft()) make.value.setScale(18) / take.value else null
}