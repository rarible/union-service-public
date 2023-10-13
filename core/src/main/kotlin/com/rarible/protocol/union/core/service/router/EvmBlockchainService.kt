package com.rarible.protocol.union.core.service.router

import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.AuctionService
import com.rarible.protocol.union.core.service.BalanceService
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.DomainService
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.SignatureService

sealed class EvmBlockchainService<T : BlockchainService>(
    val services: List<T>
)

// Injections like List<EvmService<T>> doesn't work due to "generic of generic" definition,
// so there is nothing to do - only use strict types
class EvmBalanceService(services: List<BalanceService>) : EvmBlockchainService<BalanceService>(services)
class EvmItemService(services: List<ItemService>) : EvmBlockchainService<ItemService>(services)
class EvmCollectionService(services: List<CollectionService>) : EvmBlockchainService<CollectionService>(services)
class EvmOwnershipService(services: List<OwnershipService>) : EvmBlockchainService<OwnershipService>(services)
class EvmOrderService(services: List<OrderService>) : EvmBlockchainService<OrderService>(services)
class EvmActivityService(services: List<ActivityService>) : EvmBlockchainService<ActivityService>(services)
class EvmAuctionService(services: List<AuctionService>) : EvmBlockchainService<AuctionService>(services)
class EvmDomainService(services: List<DomainService>) : EvmBlockchainService<DomainService>(services)
class EvmSignatureService(services: List<SignatureService>) : EvmBlockchainService<SignatureService>(services)
