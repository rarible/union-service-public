package com.rarible.protocol.union.integration.tezos.entity

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId
import java.math.BigInteger

@Document("tezos_token_id")
data class TezosTokenId(
    @MongoId(FieldType.STRING)
    val contract: String,
    val lastTokenId: BigInteger
)
