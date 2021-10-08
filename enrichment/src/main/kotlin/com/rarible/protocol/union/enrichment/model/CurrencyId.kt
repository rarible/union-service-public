package com.rarible.protocol.union.enrichment.model

import com.rarible.protocol.union.dto.*

data class CurrencyId(
    val blockchain: BlockchainDto,
    val address: String,
    val type: CurrencyType
) {
    override fun toString(): String {
        return "$blockchain:$type:$address"
    }

    companion object {
        fun fromString(value: String): CurrencyId {
            val parts = value.split(":")
            if (parts.size != 3) {
                throw IllegalArgumentException("Wrong CurrencyId string value $value")
            }
            return CurrencyId(
                blockchain = BlockchainDto.valueOf(parts[0]),
                type = CurrencyType.valueOf(parts[1]),
                address = parts[2]
            )
        }
    }
}
