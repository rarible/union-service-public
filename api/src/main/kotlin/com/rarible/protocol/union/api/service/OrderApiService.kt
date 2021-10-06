package com.rarible.protocol.union.api.service

import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import org.springframework.stereotype.Component

@Component
class OrderApiService(
    private val router: BlockchainRouter<OrderService>
) {

    @ExperimentalCoroutinesApi
    fun getByIds(ids: List<OrderIdDto>): Flow<OrderDto> {
        val groupedIds =
            ids.groupBy({ it.blockchain }, { it.value })

        return groupedIds
            .map { router.getService(it.key).getOrdersByIds(it.value) }
            .merge()
    }

}