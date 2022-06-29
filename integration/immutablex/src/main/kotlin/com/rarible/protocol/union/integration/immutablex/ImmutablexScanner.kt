package com.rarible.protocol.union.integration.immutablex

import com.rarible.protocol.union.integration.immutablex.client.EventsApi
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexEvent
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexPage
import com.rarible.protocol.union.integration.immutablex.entity.ImmutablexEntityTypes
import com.rarible.protocol.union.integration.immutablex.entity.ImmutablexState
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexActivityEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexItemEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexOrderEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexOwnershipEventHandler
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.scheduling.annotation.Scheduled

class ImmutablexScanner(
    private val eventsApi: EventsApi,
    private val mongo: MongoTemplate,
    private val activityHandler: ImmutablexActivityEventHandler,
    private val ownershipEventHandler: ImmutablexOwnershipEventHandler,
    private val itemEventHandler: ImmutablexItemEventHandler,
    private val orderEventHandler: ImmutablexOrderEventHandler,
) {

    @PostConstruct
    fun postCreate() {
        mongo.updateMulti(
            Query(),
            Update().unset(ImmutablexState::lastError.name).unset(ImmutablexState::lastErrorStacktrace.name),
            ImmutablexState::class.java
        )
    }

    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.mints}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS
    )
    fun mints() {
        runBlocking {
            listen(
                eventsApi::mints,
                mongo.findById(ImmutablexEntityTypes.MINTS.name, ImmutablexState::class.java) ?: mongo.save(
                    ImmutablexState(id = ImmutablexEntityTypes.MINTS.name)
                )
            )
        }
    }

    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.transfers}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS
    )
    fun transfers() {
        runBlocking {
            listen(
                eventsApi::transfers,
                mongo.findById(ImmutablexEntityTypes.TRANSFERS.name, ImmutablexState::class.java) ?: mongo.save(
                    ImmutablexState(id = ImmutablexEntityTypes.TRANSFERS.name)
                )
            )
        }
    }

    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.trades}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS
    )
    fun trades() {
        runBlocking {
            listen(
                eventsApi::trades,
                mongo.findById(ImmutablexEntityTypes.TRADES.name, ImmutablexState::class.java) ?: mongo.save(
                    ImmutablexState(id = ImmutablexEntityTypes.TRADES.name)
                )
            )
        }
    }

    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.deposits}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS
    )
    fun deposits() {
        runBlocking {
            listen(
                eventsApi::deposits,
                mongo.findById(ImmutablexEntityTypes.DEPOSITS.name, ImmutablexState::class.java) ?: mongo.save(
                    ImmutablexState(id = ImmutablexEntityTypes.DEPOSITS.name)
                )
            )
        }
    }

    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.withdrawals}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS
    )
    fun withdrawals() {
        runBlocking {
            listen(
                eventsApi::withdrawals,
                mongo.findById(ImmutablexEntityTypes.WITHDRAWALS.name, ImmutablexState::class.java) ?: mongo.save(
                    ImmutablexState(id = ImmutablexEntityTypes.WITHDRAWALS.name)
                )
            )
        }
    }

    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.orders}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS
    )
    fun orders() {
        runBlocking {
            val state = mongo.findById(ImmutablexEntityTypes.ORDERS.name, ImmutablexState::class.java) ?: mongo.save(
                ImmutablexState(id = ImmutablexEntityTypes.ORDERS.name)
            )
            try {
                val page = eventsApi.orders(state.cursor)
                if (page.result.isNotEmpty()) {
                    page.result.forEach {
                        orderEventHandler.handle(it)
                    }
                }
                mongo.save(state.copy(cursor = page.cursor))
            } catch (e: Exception) {
                mongo.save(state.copy(lastError = e.message, lastErrorStacktrace = e.stackTraceToString()))
            }
        }
    }

    private suspend fun <T : ImmutablexEvent> listen(
        apiMethod: suspend (cursor: String?) -> ImmutablexPage<T>,
        state: ImmutablexState,
    ) {
        try {
            val page = apiMethod(state.cursor)
            if (page.result.isNotEmpty()) {
                handle(page.result)
            }

            mongo.save(state.copy(cursor = page.cursor)) //todo generate cursor if empty
        } catch (e: Exception) {
            mongo.save(state.copy(lastError = e.message, lastErrorStacktrace = e.stackTraceToString()))
        }
    }

    private suspend fun <T : ImmutablexEvent> handle(items: List<T>) {
        items.forEach {
            activityHandler.handle(it)
            itemEventHandler.handle(it)
            ownershipEventHandler.handle(it)
        }
    }
}
