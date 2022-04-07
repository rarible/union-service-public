package com.rarible.protocol.union.integration.immutablex

import com.rarible.protocol.union.integration.immutablex.client.EventsApi
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexEvent
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexPage
import com.rarible.protocol.union.integration.immutablex.entity.ImmutablexState
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexActivityEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexItemEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexOrderEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexOwnershipEventHandler
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class ImmutablexScanner(
    private val eventsApi: EventsApi,
    private val mongo: MongoTemplate,
    private val activityHandler: ImmutablexActivityEventHandler,
    private val ownershipEventHandler: ImmutablexOwnershipEventHandler,
    private val itemEventHandler: ImmutablexItemEventHandler,
    private val orderEventHandler: ImmutablexOrderEventHandler,
) {

    private var info: ImmutablexState by Delegates.notNull()

    @PostConstruct
    fun postCreate() {
        info = mongo.findById(1L) ?: mongo.save(ImmutablexState(id = 1L))
    }

    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.mints}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS)
    fun mints() {
        runBlocking { listen(eventsApi::mints, info::lastMintCursor) }
    }

    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.transfers}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS)
    fun transfers() {
        runBlocking { listen(eventsApi::transfers, info::lastTransferCursor) }
    }

    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.trades}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS)
    fun trades() {
        runBlocking { listen(eventsApi::trades, info::lastTradesCursor) }
    }

    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.deposits}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS)
    fun deposits() {
        runBlocking { listen(eventsApi::deposits, info::lastDepositCursor) }
    }

    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.withdrawals}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS)
    fun withdrawals() {
        runBlocking { listen(eventsApi::withdrawals, info::lastWithdrawCursor) }
    }

    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.orders}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS)
    fun orders() {
        runBlocking {
            val page = eventsApi.orders(info.lastOrderCursor)
            if (page.result.isNotEmpty()) {
                page.result.forEach {
                    orderEventHandler.handle(it)
                }
            }
            mongo.updateFirst(Query(
                where(ImmutablexState::id).isEqualTo(1L)
            ), Update().set(info::lastOrderCursor.name, page.cursor), ImmutablexState::class.java)
        }
    }

    private suspend fun <T : ImmutablexEvent> listen(
        apiMethod: suspend (cursor: String?) -> ImmutablexPage<T>,
        cursorField: KProperty<String?>,
    ) {
        val page = apiMethod(cursorField.call())
        if (page.result.isNotEmpty()) {
            handle(page.result)
        }
        mongo.updateFirst(Query(
            where(ImmutablexState::id).isEqualTo(1L)
        ), Update().set(cursorField.name, page.cursor), ImmutablexState::class.java)

    }

    private suspend fun <T : ImmutablexEvent> handle(items: List<T>) {
        items.forEach {
            activityHandler.handle(it)
            itemEventHandler.handle(it)
            ownershipEventHandler.handle(it)
        }
    }
}
