package com.rarible.protocol.union.enrichment.meta.simplehash

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

data class SimpleHashNftMetadataUpdate(
    val type: HookEventType,
    val nfts: List<SimpleHashItem>
)

sealed class HookEventType(@get:JsonValue val value: String) {
    object ChainNftMetadataUpdate : HookEventType("chain.nft_metadata_update")

    object ContractNftMetadataUpdate : HookEventType("contract.nft_metadata_update")

    class Unknown(values: String) : HookEventType(values)

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): HookEventType {
            return when (value) {
                ChainNftMetadataUpdate.value -> ChainNftMetadataUpdate
                ContractNftMetadataUpdate.value -> ContractNftMetadataUpdate
                else -> Unknown(value)
            }
        }
    }
}
