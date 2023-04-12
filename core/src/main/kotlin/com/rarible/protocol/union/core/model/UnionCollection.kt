package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.UnionAddress

data class UnionCollection(
    val id: CollectionIdDto,
    val name: String,
    val status: Status? = null,
    val structure: Structure = Structure.REGULAR,
    val type: Type,
    val minters: List<UnionAddress>? = listOf(),
    val features: List<Features> = listOf(),
    // TODO remove later
    val meta: UnionCollectionMeta? = null,
    val owner: UnionAddress? = null,
    val parent: CollectionIdDto? = null,
    val symbol: String? = null,
    val self: Boolean? = null,
) {

    enum class Features {
        APPROVE_FOR_ALL,
        SET_URI_PREFIX,
        BURN,
        MINT_WITH_ADDRESS,
        SECONDARY_SALE_FEES,
        MINT_AND_TRANSFER
    }

    enum class Structure {
        REGULAR,
        COMPOSITE,
        PART
    }

    enum class Type {
        CRYPTO_PUNKS,
        ERC721,
        ERC1155,
        FLOW,
        TEZOS_NFT,
        TEZOS_MT,
        SOLANA,
        IMMUTABLEX
    }

    enum class Status {
        PENDING,
        ERROR,
        CONFIRMED
    }

}