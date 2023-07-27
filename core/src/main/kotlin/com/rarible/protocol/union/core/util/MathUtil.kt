package com.rarible.protocol.union.core.util

import com.rarible.protocol.union.core.model.UnionAsset
import java.math.BigDecimal

fun evalMakePrice(make: UnionAsset, take: UnionAsset): BigDecimal? {
    return if (make.type.isNft()) take.value.setScale(18) / make.value else null
}

fun evalTakePrice(make: UnionAsset, take: UnionAsset): BigDecimal? {
    return if (take.type.isNft()) make.value.setScale(18) / take.value else null
}
