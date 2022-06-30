package com.rarible.protocol.union.integration.tezos.entity

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId
import java.math.BigInteger

@Document("tezos_collection")
data class TezosCollection(
    @MongoId(FieldType.STRING)
    val contract: String,
    val type: Type?,
    val lastTokenId: BigInteger
) {
    enum class Type {
        NFT, MT
    }
}
