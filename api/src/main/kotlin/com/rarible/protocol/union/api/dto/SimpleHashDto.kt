package com.rarible.protocol.union.api.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue

data class SimpleHashNftMetadataUpdateDto(
    val type: HookEventType,
    val nfts: List<SimpleHashNft>
)

data class SimpleHashNft(
    @JsonProperty("nft_id")
    val itemId: String
)

sealed class HookEventType(@get:JsonValue val value: String) {
    object ChainNftMetadataUpdate : HookEventType("chain.nft_metadata_update")

    class Unknown(values: String) : HookEventType(values)

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): HookEventType {
            return when (value) {
                ChainNftMetadataUpdate.value -> ChainNftMetadataUpdate
                else -> Unknown(value)
            }
        }
    }
}