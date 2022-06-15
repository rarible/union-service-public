package com.rarible.protocol.union.integration.tezos.entity

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import java.math.BigInteger

class TezosTokenIdRepository(
    private val mongo: ReactiveMongoOperations
) {
    suspend fun generateNftTokenId(collectionId: String): BigInteger {
        return mongo.findAndModify(
            Query(where(TezosTokenId::contract).isEqualTo(collectionId)),
            Update().inc(TezosTokenId::lastTokenId.name, 1),
            FindAndModifyOptions.options().returnNew(true).upsert(true),
            TezosTokenId::class.java
        ).awaitSingle().lastTokenId
    }
}
