package com.rarible.protocol.union.integration.immutablex

import com.rarible.protocol.union.integration.immutablex.client.EventsApi
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexPage
import com.rarible.protocol.union.integration.immutablex.entity.ImmutablexState
import com.rarible.protocol.union.integration.immutablex.kafka.ImmutablexInternalProducer
import kotlinx.coroutines.*
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import kotlin.reflect.KProperty

class ImmutablexScanner(
    private val eventsApi: EventsApi,
    private val mongo: MongoTemplate,
    private val producer: ImmutablexInternalProducer,
) {

    private lateinit var info: ImmutablexState

    @PostConstruct
    fun postCreate() {
        info = mongo.findById(1L) ?: mongo.save(ImmutablexState(id = 1L))
    }

    @Scheduled(initialDelay = 5L, fixedDelay = 60L, timeUnit = TimeUnit.SECONDS)
    fun mints() {
        runBlocking { listen(eventsApi::mints, producer::mints, info::lastMintCursor) }
    }
    @Scheduled(initialDelay = 5L, fixedDelay = 60L, timeUnit = TimeUnit.SECONDS)
    fun transfers() {
        runBlocking { listen(eventsApi::transfers, producer::transfers, info::lastTransferCursor) }
    }
    @Scheduled(initialDelay = 5L, fixedDelay = 60L, timeUnit = TimeUnit.SECONDS)
    fun trades() {
        runBlocking { listen(eventsApi::trades, producer::trades, info::lastTradesCursor) }
    }

    @Scheduled(initialDelay = 5L, fixedDelay = 60L, timeUnit = TimeUnit.SECONDS)
    fun deposits() {
        runBlocking { listen(eventsApi::deposits, producer::deposits, info::lastDepositCursor) }
    }

    @Scheduled(initialDelay = 5L, fixedDelay = 60L, timeUnit = TimeUnit.SECONDS)
    fun withdrawals() {
        runBlocking { listen(eventsApi::withdrawals, producer::withdrawals, info::lastWithdrawCursor) }
    }

    private suspend fun <T>listen(apiMethod: suspend (cursor: String?) -> ImmutablexPage<T>, producerMethod: suspend (items: List<T>) -> Unit, cursorField: KProperty<String?>) {
        var page = apiMethod(cursorField.call())
        while (page.remaining) {
            producerMethod(page.result)
            mongo.updateFirst(Query(
                where(ImmutablexState::id).isEqualTo(1L)
            ), Update().set(cursorField.name, page.cursor), ImmutablexState::class.java)
            page = apiMethod(page.cursor)
        }
        if (page.result.isNotEmpty()) {
            producerMethod(page.result)
        }
    }
}
