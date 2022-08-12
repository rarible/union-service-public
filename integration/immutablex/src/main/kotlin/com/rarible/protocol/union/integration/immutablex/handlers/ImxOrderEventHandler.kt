package com.rarible.protocol.union.integration.immutablex.handlers

import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrder
import com.rarible.protocol.union.integration.immutablex.converter.ImxDataException
import com.rarible.protocol.union.integration.immutablex.converter.ImxOrderConverter
import com.rarible.protocol.union.integration.immutablex.scanner.ImxScanEntityType
import com.rarible.protocol.union.integration.immutablex.scanner.ImxScanMetrics
import org.slf4j.LoggerFactory

class ImxOrderEventHandler(
    override val handler: IncomingEventHandler<UnionOrderEvent>,
    private val imxScanMetrics: ImxScanMetrics,
) : AbstractBlockchainEventHandler<ImmutablexOrder, UnionOrderEvent>(BlockchainDto.IMMUTABLEX) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: ImmutablexOrder) {
        val order = try {
            ImxOrderConverter.convert(event, blockchain)
        } catch (e: ImxDataException) {
            // It should not happen on prod, but if there is inconsistent data we can just skip it
            // and then report to IMX support
            logger.error("Failed to process Order (invalid data), skipped: {}, error: {}", event, e.message)
            imxScanMetrics.onEventError(ImxScanEntityType.ORDER.name)
            return
        }
        handler.onEvent(UnionOrderUpdateEvent(order))
    }
}
