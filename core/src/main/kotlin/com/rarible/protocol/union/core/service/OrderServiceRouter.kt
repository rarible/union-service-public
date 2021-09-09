package com.rarible.protocol.union.core.service

import org.springframework.stereotype.Component

@Component
class OrderServiceRouter(
    orderServices: List<OrderService>
) : BlockchainRouter<OrderService>(
    orderServices
)