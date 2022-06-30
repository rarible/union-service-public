package com.rarible.protocol.union.integration.tezos.entity

import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.awaitSingle
import org.bson.types.Decimal128
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import java.math.BigInteger

class TezosCollectionRepository(
    private val mongo: ReactiveMongoOperations
) {
    suspend fun generateTokenId(collectionId: String): BigInteger {
        return mongo.findAndModify(
            Query(where(TezosCollection::contract).isEqualTo(collectionId)),
            Update().inc(TezosCollection::lastTokenId.name, 1),
            FindAndModifyOptions.options().returnNew(true).upsert(true),
            TezosCollection::class.java
        ).awaitSingle().lastTokenId
    }

    suspend fun adjustTokenCount(collectionId: String, count: BigInteger): BigInteger {
        return mongo.findAndModify(
            Query(where(TezosCollection::contract).isEqualTo(collectionId)),
            Update().max(TezosCollection::lastTokenId.name, Decimal128.parse(count.toString())),
            FindAndModifyOptions.options().returnNew(true).upsert(true),
            TezosCollection::class.java
        ).awaitSingle().lastTokenId
    }

    suspend fun adjustCollectionType(collectionId: String, type: TezosCollection.Type) {
        mongo.findAndModify(
            Query(where(TezosCollection::contract).isEqualTo(collectionId).and(TezosCollection::lastTokenId.name).gt(0)),
            Update().set(TezosCollection::type.name, type).set(TezosCollection::lastTokenId.name, 0),
            FindAndModifyOptions.options().returnNew(true).upsert(true),
            TezosCollection::class.java
        ).awaitSingle()
    }

    suspend fun getCollections(ids: List<String>): List<TezosCollection> {
        val query = Query(TezosCollection::contract inValues ids)
        return mongo.query<TezosCollection>().matching(query)
            .all()
            .collectList()
            .awaitFirst()
    }
}
