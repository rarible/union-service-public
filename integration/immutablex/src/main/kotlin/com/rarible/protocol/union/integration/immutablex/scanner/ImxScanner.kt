package com.rarible.protocol.union.integration.immutablex.scanner

import com.rarible.core.common.nowMillis
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexEvent
import com.rarible.protocol.union.integration.immutablex.client.TokenIdDecoder
import com.rarible.protocol.union.integration.immutablex.handlers.ImxActivityEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImxCollectionEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImxItemEventHandler
import com.rarible.protocol.union.integration.immutablex.handlers.ImxOrderEventHandler
import kotlinx.coroutines.runBlocking
import org.springframework.scheduling.annotation.Scheduled
import scalether.domain.Address
import java.util.concurrent.TimeUnit

class ImxScanner(
    private val imxEventsApi: ImxEventsApi,
    private val imxScanStateRepository: ImxScanStateRepository,
    private val imxScanMetrics: ImxScanMetrics,

    private val activityHandler: ImxActivityEventHandler,
    private val itemEventHandler: ImxItemEventHandler,
    private val orderEventHandler: ImxOrderEventHandler,
    private val collectionEventHandler: ImxCollectionEventHandler
) {

    private val logger by Logger()

    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.mints}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS
    )
    fun mints() = listenActivity(ImxScanEntityType.MINT, imxEventsApi::mints) {
        imxEventsApi.lastMint().transactionId.toString()
    }

    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.transfers}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS
    )
    fun transfers() = listenActivity(ImxScanEntityType.TRANSFER, imxEventsApi::transfers) {
        imxEventsApi.lastTransfer().transactionId.toString()
    }

    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.trades}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS
    )
    fun trades() = listenActivity(ImxScanEntityType.TRADE, imxEventsApi::trades) {
        imxEventsApi.lastTrade().transactionId.toString()
    }

    private fun <T : ImmutablexEvent> listenActivity(
        type: ImxScanEntityType,
        getEvents: suspend (id: String) -> List<T>,
        getInitialId: suspend () -> String,
    ) = listen(type, getInitialId) { state ->
        // We want to start from last known activity, other activities will
        // be synced via jobs
        val transactionId = state.entityId

        val page = getEvents(transactionId)
        page.forEach { activityHandler.handle(it) }

        val last = page.lastOrNull() ?: return@listen null

        ImxScanResult(last.transactionId.toString(), last.timestamp)
    }

    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.orders}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS
    )
    fun orders() = listen(ImxScanEntityType.ORDER, { "0" }, { state ->
        val page = imxEventsApi.orders(state.entityDate, state.entityId)
        page.forEach { orderEventHandler.handle(it) }

        val last = page.lastOrNull() ?: return@listen null
        ImxScanResult(last.orderId.toString(), last.updatedAt!!)
    })

    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.assets}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS
    )
    fun assets() = listen(ImxScanEntityType.ITEM, { "${Address.ZERO().prefixed()}:0" }) { state ->
        val page = imxEventsApi.assets(state.entityDate, TokenIdDecoder.decodeItemId(state.entityId))
        page.forEach { itemEventHandler.handle(it) }

        val last = page.lastOrNull() ?: return@listen null
        ImxScanResult(last.encodedItemId(), last.updatedAt!!)
    }

    @Scheduled(
        initialDelayString = "\${integration.immutablex.scanner.job.initialDelay.collections}",
        fixedDelayString = "\${integration.immutablex.scanner.job.fixedDelay}",
        timeUnit = TimeUnit.SECONDS
    )
    fun collections() = listen(ImxScanEntityType.COLLECTION, { Address.ZERO().prefixed() }) { state ->
        val page = imxEventsApi.collections(state.entityDate, state.entityId)
        page.forEach { collectionEventHandler.handle(it) }

        val last = page.lastOrNull() ?: return@listen null
        ImxScanResult(last.address, nowMillis()) // TODO replace with update_at when IMX add it
    }

    private fun listen(
        type: ImxScanEntityType,
        getInitialId: suspend () -> String,
        handle: suspend (state: ImxScanState) -> ImxScanResult?,
    ) = runBlocking {
        // We should download all entities without until we reach the last one
        while (true) {
            val state = imxScanStateRepository.getState(type)
                ?: imxScanStateRepository.createState(type, getInitialId())

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
