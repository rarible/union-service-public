package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.enrichment.model.CurrencyId

object CurrencyIdConverter {
    fun convert(assetType: AssetTypeDto): CurrencyId {
        return CurrencyId(assetType.contract.blockchain, assetType.contract.value)
    }
}
