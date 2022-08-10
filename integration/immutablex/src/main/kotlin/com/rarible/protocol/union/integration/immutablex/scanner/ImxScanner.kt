package com.rarible.protocol.union.integration.immutablex.scanner

import com.rarible.core.common.nowMillis
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexEvent
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexActivityEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexItemEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexOrderEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImmutablexOwnershipEventHandler
import kotlinx.coroutines.runBlocking
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.TimeUnit

class ImxScanner(
    private val imxEventsApi: ImxEventsApi,
    private val imxScanStateRepository: ImxScanStateRepository,
    private val imxScanMetrics: ImxScanMetrics,

    private val activityHandler: ImmutablexActivityEventHandler,
    private val ownershipEventHandler: ImmutablexOwnershipEventHandler,
    private val itemEventHandler: ImmutablexItemEventHandler,
    private val orderEventHandler: ImmutablexOrderEventHandler,
) {

    private val logger by Logger()

    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.mints}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS
    )
    fun mints() = listenActivity(imxEventsApi::mints, imxEventsApi::lastMint, ImxScanEntityType.MINTS)

    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.transfers}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS
    )
    fun transfers() = listenActivity(imxEventsApi::transfers, imxEventsApi::lastTransfer, ImxScanEntityType.TRANSFERS)

    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.trades}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS
    )
    fun trades() = listenActivity(imxEventsApi::trades, imxEventsApi::lastTrade, ImxScanEntityType.TRADES)

    private fun <T : ImmutablexEvent> listenActivity(
        apiMethod: suspend (id: String) -> List<T>,
        getInitialId: suspend () -> T,
        type: ImxScanEntityType,
    ) = listen(type) { state ->
        // We want to start from last known activity, other activities will
        // be synced via jobs
        val transactionId = state.entityId ?: getInitialId().transactionId.toString()

        val page = apiMethod(transactionId)
        handle(page)

        val last = page.lastOrNull() ?: return@listen null

        ImxScanResult(last.transactionId.toString(), last.timestamp)
    }

    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.orders}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS
    )
    fun orders() = listen(ImxScanEntityType.ORDERS) { state ->
        val date = state.entityDate ?: nowMillis()
        val id = state.entityId ?: "0"
        val page = imxEventsApi.orders(date, id)
        page.forEach { orderEventHandler.handle(it) }

        val last = page.lastOrNull() ?: return@listen null
        ImxScanResult(last.orderId.toString(), last.updatedAt!!)
    }

    private fun listen(
        type: ImxScanEntityType,
        handle: suspend (state: ImxScanState) -> ImxScanResult?,
    ) = runBlocking {
        // We should download all entities without until we reach the last one
        while (true) {
            val state = imxScanStateRepository.getOrCreateState(type)
            try {
                val result = handle(state)

                // We need to keep last cursor position (and entity date) if we've reached the end
                val entityDate = result?.entityDate ?: state.entityDate
                val entityId = result?.entityId ?: state.entityId

                imxScanStateRepository.updateState(state, entityDate, entityId)
                imxScanMetrics.onStateUpdated(state)

                // All entities are read, now we can delay scanner a bit before try again
                if (result == null) {
                    logger.info("Immutablex scan for {} reached last actual entity with date {}", type, entityDate)
                    return@runBlocking
                }
            } catch (e: Exception) {
                // On error, we should make a delay too in order to do not spam IMX with bad requests
                // and ELK with error logs
                logger.error("Failed to get Immutablex events: {}", type, e)
                imxScanStateRepository.updateState(state, e)

                // TODO IMMUTABLEX i think here we can provide more details for different errors in future
                imxScanMetrics.onScanError(state, "unknown")
                return@runBlocking
            }
        }
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
        listenActivity(eventsApi::deposits, ImmutablexEntityTypes.DEPOSITS)
    }

    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.withdrawals}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS
    )
    fun withdrawals() = runBlocking {
        listenActivity(eventsApi::withdrawals, ImmutablexEntityTypes.WITHDRAWALS)
    }*/

}
