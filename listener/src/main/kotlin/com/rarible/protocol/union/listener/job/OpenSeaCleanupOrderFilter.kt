package com.rarible.protocol.union.listener.job

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.enrichment.service.EnrichmentOrderService
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class OpenSeaCleanupOrderFilter(
    private val orderService: EnrichmentOrderService
) {

    private val from = Instant.now().minus(10, ChronoUnit.DAYS)

    suspend fun isOld(blockchain: BlockchainDto, orderId: String): Boolean {
        val order = orderService.getById(OrderIdDto(blockchain, orderId))
        return order != null && order.createdAt.isBefore(from)
    }

}