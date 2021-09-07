package com.rarible.protocol.union.core.service

import org.springframework.stereotype.Component

@Component
class ItemServiceRouter(
    itemServices: List<ItemService>
) : BlockchainRouter<ItemService>(
    itemServices
)