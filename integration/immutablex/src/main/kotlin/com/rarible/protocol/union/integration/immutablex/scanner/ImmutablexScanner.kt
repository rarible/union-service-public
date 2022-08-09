package com.rarible.protocol.union.integration.immutablex.scanner

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexEvent
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexPage
import com.rarible.protocol.union.integration.immutablex.entity.ImmutablexEntityTypes
import com.rarible.protocol.union.integration.immutablex.entity.ImmutablexState
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexActivityEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexItemEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexOrderEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexOwnershipEventHandler
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.scheduling.annotation.Scheduled
import java.time.Instant
import java.util.concurrent.TimeUnit

class ImmutablexScanner(
    private val eventsApi: EventsApi,
    private val mongo: ReactiveMongoTemplate,
    private val activityHandler: ImmutablexActivityEventHandler,
    private val ownershipEventHandler: ImmutablexOwnershipEventHandler,
    private val itemEventHandler: ImmutablexItemEventHandler,
    private val orderEventHandler: ImmutablexOrderEventHandler,
) {

    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.mints}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS
    )
    fun mints() = runBlocking {
        listen(eventsApi::mints, getOrCreateState(ImmutablexEntityTypes.MINTS))
    }

    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.transfers}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS
    )
    fun transfers() = runBlocking {
        listen(eventsApi::transfers, getOrCreateState(ImmutablexEntityTypes.TRANSFERS))
    }

    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.trades}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS
    )
    fun trades() = runBlocking {
        listen(eventsApi::trades, getOrCreateState(ImmutablexEntityTypes.TRADES))
    }

    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.orders}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS
    )
    fun orders() = runBlocking {
        val state = getOrCreateState(ImmutablexEntityTypes.ORDERS)
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

            val entityDate = page.lastOrNull()?.updatedAt ?: state.entityDate

            updateState(state, cursor, entityDate)
        } catch (e: Exception) {
            updateState(state, e)
        }
    }

    private suspend fun <T : ImmutablexEvent> listen(
        apiMethod: suspend (cursor: String?) -> ImmutablexPage<T>,
        state: ImmutablexState,
    ) {
        try {
            val page = apiMethod(state.cursor)

            handle(page.result)

            val cursor = page.cursor.ifBlank {
                state.cursor // Keep the latest cursor if we reached the newest entity
            }

            val entityDate = page.result.lastOrNull()?.timestamp ?: state.entityDate

            updateState(state, cursor, entityDate)
        } catch (e: Exception) {
            updateState(state, e)
        }
    }

    private suspend fun getOrCreateState(type: ImmutablexEntityTypes): ImmutablexState {
        val id = type.name
        return mongo.findById(id, ImmutablexState::class.java).awaitFirstOrNull()
            ?: mongo.save(ImmutablexState(id = id)).awaitFirst()
    }

    private suspend fun updateState(state: ImmutablexState, cursor: String?, entityDate: Instant?) {
        mongo.save(
            state.copy(
                cursor = cursor,
                entityDate = entityDate
            )
        ).awaitFirstOrNull()
    }

    private suspend fun updateState(state: ImmutablexState, error: Exception) {
        mongo.save(
            state.copy(
                lastError = error.message,
                lastErrorDate = nowMillis(),
                lastErrorStacktrace = error.stackTraceToString()
            )
        ).awaitFirstOrNull()
    }

    private suspend fun <T : ImmutablexEvent> handle(items: List<T>) {
        items.forEach {
            activityHandler.handle(it)
            itemEventHandler.handle(it)
            ownershipEventHandler.handle(it)
        }
    }

    // We don't need these events ATM
    /*
    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.deposits}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS
    )
    fun deposits() = runBlocking {
        listen(eventsApi::deposits, getOrCreateState(ImmutablexEntityTypes.DEPOSITS))
    }

    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.withdrawals}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS
    )
    fun withdrawals() = runBlocking {
        listen(eventsApi::withdrawals, getOrCreateState(ImmutablexEntityTypes.WITHDRAWALS))
    }*/

}
