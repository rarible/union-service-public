package com.rarible.protocol.union.search.core.repository

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import co.elastic.clients.elasticsearch._types.OpType
import co.elastic.clients.elasticsearch.core.GetRequest
import co.elastic.clients.elasticsearch.core.IndexRequest
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.search.core.syntax.get
import kotlinx.coroutines.future.await
import kotlinx.coroutines.reactive.awaitFirst
import reactor.kotlin.core.publisher.toMono
import java.time.Instant

interface EsRepository {
    val index: String
}

//TODO replace with proper data class
data class ElasticActivity(
    val activityId: String,
    // Sort fields
    val date: Instant,
    val blockNumber: Long?,
    val logIndex: Long?,
    // Filter fields
    val blockchain: BlockchainDto,
    val type: ActivityTypeDto,
    val user: User,
    val collection: Collection,
    val item: Item,

    ) {
    data class User(
        val maker: String,
        val taker: String?,
    )

    data class Collection(
        val make: String,
        val take: String?
    )

    data class Item(
        val make: String,
        val take: String?
    )
}

class ActivityEsRepository(
    private val elasticsearchAsyncClient: ElasticsearchAsyncClient
): EsRepository {

    override val index: String
        get() = INDEX

    suspend fun putDocument(esActivity: ElasticActivity): String {
        return elasticsearchAsyncClient.index { builder: IndexRequest.Builder<ElasticActivity> ->
            builder
                .index(index)
                .document(esActivity)
                .opType(OpType.Create)
        }.toMono().awaitFirst().id()
    }

    suspend fun getById(id: String): ElasticActivity? {
        return elasticsearchAsyncClient.get<ElasticActivity>(
            GetRequest.of { builder ->
                builder.id(id).index(index)
            }
        )?.source()
    }

    companion object {
        const val INDEX = "activity"
    }
}