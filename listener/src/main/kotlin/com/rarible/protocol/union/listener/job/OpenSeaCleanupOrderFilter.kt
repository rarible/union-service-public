package com.rarible.protocol.union.listener.job

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.enrichment.service.EnrichmentOrderService
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class OpenSeaCleanupOrderFilter(
    private val orderService: EnrichmentOrderService
) {
    suspend fun isOld(blockchain: BlockchainDto, orderId: String, from: Instant): Boolean {
        val order = orderService.getById(OrderIdDto(blockchain, orderId))
        return order != null && order.createdAt.isBefore(from)
    }

}