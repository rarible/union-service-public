package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipSearchRequestDto
import com.rarible.protocol.union.dto.UnionAddress
import java.time.Instant

sealed interface EsOwnershipFilter {
    val cursor: String?
}

data class EsOwnershipByOwnerFilter(
    val owner: UnionAddress,
    val blockchains: Collection<BlockchainDto>? = null,
    override val cursor: String? = null
) : EsOwnershipFilter

data class EsOwnershipByItemFilter(
    val itemId: ItemIdDto,
    override val cursor: String? = null
) : EsOwnershipFilter

data class EsOwnershipsSearchFilter(
    override val cursor: String? = null,
    val blockchains: Set<BlockchainDto>? = null,
    val collections: List<CollectionIdDto>? = null,
    val owners: List<UnionAddress>? = null,
    val items: List<ItemIdDto>? = null,
    val auctions: List<AuctionIdDto>? = null,
    val auctionOwners: List<UnionAddress>? = null,
    val beforeDate: Instant? = null,
    val afterDate: Instant? = null
) : EsOwnershipFilter {
    constructor(request: OwnershipSearchRequestDto) : this(
        cursor = request.continuation,
        blockchains = request.filter.blockchains?.toSet(),
        collections = request.filter.collections,
        owners = request.filter.owners,
        items = request.filter.items,
        auctions = request.filter.auctions,
        auctionOwners = request.filter.auctionsOwners,
        beforeDate = request.filter.beforeDate,
        afterDate = request.filter.afterDate
    )
}
