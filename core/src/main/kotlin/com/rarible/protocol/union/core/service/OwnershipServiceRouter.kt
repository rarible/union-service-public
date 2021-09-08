package com.rarible.protocol.union.core.service

import org.springframework.stereotype.Component

@Component
class OwnershipServiceRouter(
    ownershipServices: List<OwnershipService>
) : BlockchainRouter<OwnershipService>(
    ownershipServices
)