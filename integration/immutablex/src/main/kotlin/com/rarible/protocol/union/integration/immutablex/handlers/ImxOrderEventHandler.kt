package com.rarible.protocol.union.integration.immutablex.handlers

import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.core.model.blockchainAndIndexerMarks
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrder
import com.rarible.protocol.union.integration.immutablex.converter.ImxDataException
import com.rarible.protocol.union.integration.immutablex.converter.ImxOrderV3Converter
import com.rarible.protocol.union.integration.immutablex.scanner.ImxScanEntityType
import com.rarible.protocol.union.integration.immutablex.scanner.ImxScanMetrics
import org.slf4j.LoggerFactory

class ImxOrderEventHandler(
    private val handler: IncomingEventHandler<UnionOrderEvent>,
    private val imxScanMetrics: ImxScanMetrics,
    private val imxOrderConverter: ImxOrderV3Converter
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val blockchain = BlockchainDto.IMMUTABLEX

    suspend fun handle(events: List<ImmutablexOrder>) {
        events.forEach { event ->
            val order = try {
                imxOrderConverter.convert(event, blockchain)
            } catch (e: ImxDataException) {
                // It should not happen on prod, but if there is inconsistent data we can just skip it
                // and then report to IMX support
                logger.error("Failed to process Order (invalid data), skipped: {}, error: {}", event, e.message)
                imxScanMetrics.onEventError(ImxScanEntityType.ORDER.name)
                return
            }
            val eventTimeMarks = blockchainAndIndexerMarks(order.lastUpdatedAt)
            handler.onEvent(UnionOrderUpdateEvent(order, eventTimeMarks))
        }
    }
}
