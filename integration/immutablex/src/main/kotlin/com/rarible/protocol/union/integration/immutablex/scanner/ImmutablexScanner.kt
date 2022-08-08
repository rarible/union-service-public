package com.rarible.protocol.union.integration.immutablex.scanner

import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexEvent
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexPage
import com.rarible.protocol.union.integration.immutablex.entity.ImmutablexEntityTypes
import com.rarible.protocol.union.integration.immutablex.entity.ImmutablexState
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexActivityEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexItemEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexOrderEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexOwnershipEventHandler
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

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
                page.forEach { orderEventHandler.handle(it) }
                // We can't use native cursor here, it can be too large for GET request
                val cursor = page.lastOrNull()?.let {
                    // originally, updatedAt is never null, but it can be equal to createdAt
                    DateIdContinuation(it.updatedAt!!, it.orderId.toString()).toString()
                    // We should NOT update cursor if we got the last page
                    // in order to prevent starting cycle of scan from zero
                } ?: state.cursor

                mongo.save(state.copy(cursor = cursor.toString()))

            } catch (e: Exception) {
                mongo.save(state.copy(lastError = e.message, lastErrorStacktrace = e.stackTraceToString()))
            }
        }
    }

    // We don't need these events ATM
    /*
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
    */

    private suspend fun <T : ImmutablexEvent> listen(
        apiMethod: suspend (cursor: String?) -> ImmutablexPage<T>,
        state: ImmutablexState,
    ) {
        try {
            val page = apiMethod(state.cursor)
            if (page.result.isNotEmpty()) {
                handle(page.result)
            }
            val cursor = page.cursor.ifBlank {
                state.cursor // Keep the latest cursor if we reached the newest entity
            }

            mongo.save(state.copy(cursor = cursor))
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
