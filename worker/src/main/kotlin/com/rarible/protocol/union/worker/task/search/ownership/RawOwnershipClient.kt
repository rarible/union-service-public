package com.rarible.protocol.union.worker.task.search.ownership

import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.AuctionService
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.AuctionSortDto
import com.rarible.protocol.union.dto.AuctionStatusDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import org.springframework.stereotype.Component

@Component
class RawOwnershipClient(
    private val auctionServiceRouter: BlockchainRouter<AuctionService>,
    private val ownershipServiceRouter: BlockchainRouter<OwnershipService>,
) {

    companion object {
        private val logger by Logger()
    }

    suspend fun getRawOwnershipsAll(
        blockchain: BlockchainDto,
        continuation: String?,
        size: Int,
    ): Slice<UnionOwnership> {
        logger.info("Requested to get all ownerships, blockchain ${blockchain.name}, cursor $continuation, size $size")
        return ownershipServiceRouter.getService(blockchain).getOwnershipsAll(
            continuation = continuation,
            size = size
        ).also {
            logger.info("Got ${it.entities.size} ownerships, blockchain ${blockchain.name}, cursor ${it.continuation}")
        }
    }

    suspend fun getAuctionAll(
        blockchain: BlockchainDto,
        continuation: String?,
        size: Int,
    ): Slice<AuctionDto> =
        auctionServiceRouter.getService(blockchain).getAuctionsAll(
            sort = AuctionSortDto.LAST_UPDATE_DESC,
            status = listOf(AuctionStatusDto.ACTIVE),
            origin = null,
            platform = null,
            continuation = continuation,
            size = size,
        )
}
