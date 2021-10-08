package com.rarible.protocol.union.api.service

import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.springframework.stereotype.Component

@Component
class OrderApiService(
    private val router: BlockchainRouter<OrderService>
) {

    @ExperimentalCoroutinesApi
    suspend fun getByIds(ids: List<OrderIdDto>): List<OrderDto> {
        val groupedIds = ids.groupBy({ it.blockchain }, { it.value })

        return groupedIds.flatMap {
            router.getService(it.key).getOrdersByIds(it.value)
        }
    }

}