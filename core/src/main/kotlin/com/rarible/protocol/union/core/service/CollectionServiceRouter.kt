package com.rarible.protocol.union.core.service

import org.springframework.stereotype.Component

@Component
class CollectionServiceRouter(
    collectionServices: List<CollectionService>
) : BlockchainRouter<CollectionService>(
    collectionServices
)