package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.core.service.AuctionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter

// TODO AUCTIONS DISABLED
// @RestController
class AuctionController(
    private val router: BlockchainRouter<AuctionService>
) /*: AuctionControllerApi*/ {

    /*
        private val logger = LoggerFactory.getLogger(javaClass)

        private val empty = AuctionsDto(0, null, emptyList())

        override suspend fun getAuctionBidsById(
            id: String,
            continuation: String?,
            size: Int?
        ): ResponseEntity<AuctionBidsDto> {
            val auctionId = IdParser.parseAuctionId(id)
            val slice = router.getService(auctionId.blockchain).getAuctionsBidsById(auctionId.value, continuation, size)
            return ResponseEntity.ok(toDto(slice))
        }

        override suspend fun getAuctionById(id: String): ResponseEntity<AuctionDto> {
            val auctionId = IdParser.parseAuctionId(id)
            val result = router.getService(auctionId.blockchain).getAuctionById(auctionId.value)
            return ResponseEntity.ok(result)
        }

        override suspend fun getAuctionsAll(
            blockchains: List<BlockchainDto>?,
            sort: AuctionSortDto?,
            status: List<AuctionStatusDto>?,
            origin: String?,
            platform: PlatformDto?,
            continuation: String?,
            size: Int?
        ): ResponseEntity<AuctionsDto> {
            val safeSize = PageSize.AUCTION.limit(size)
            val originAddress = safeAddress(origin)
            val filter = BlockchainFilter(blockchains)

            val evaluatedBlockchains = originAddress?.blockchainGroup?.let { filter.exclude(it) } ?: blockchains

            val blockchainSlices = router.executeForAll(evaluatedBlockchains) {
                it.getAuctionsAll(sort, status, origin, platform, continuation, size)
            }

            val combinedSlice = Paging(
                AuctionContinuation.ByLastUpdateDesc,
                blockchainSlices.flatMap { it.entities }
            ).getSlice(safeSize)

            logger.info("Response for getAuctionsAll(blockchains={}, continuation={}, size={}):" +
                    " Slice(size={}, continuation={}) from blockchain slices {} ",
                evaluatedBlockchains, continuation, size,
                combinedSlice.entities.size, combinedSlice.continuation, blockchainSlices.map { it.entities.size }
            )

            return ResponseEntity.ok(toDto(combinedSlice))
        }

        override suspend fun getAuctionsByCollection(
            contract: String,
            seller: String?,
            origin: String?,
            status: List<AuctionStatusDto>?,
            platform: PlatformDto?,
            continuation: String?,
            size: Int?
        ): ResponseEntity<AuctionsDto> {
            val contractId = IdParser.parseAuctionId(contract)
            val sellerAddress = safeAddress(seller)
            val originAddress = safeAddress(origin)
            val slice = router.getService(contractId.blockchain).getAuctionsByCollection(
                contractId.value,
                sellerAddress?.value,
                originAddress?.value,
                status,
                platform,
                continuation,
                size
            )
            return ResponseEntity.ok(toDto(slice))
        }

        override suspend fun getAuctionsByItem(
            itemId: String,
            seller: String?,
            sort: AuctionSortDto?,
            origin: String?,
            status: List<AuctionStatusDto>?,
            platform: PlatformDto?,
            continuation: String?,
            size: Int?
        ): ResponseEntity<AuctionsDto> {
            val fullItemId = IdParser.parseItemId(itemId)
            val sellerAddress = safeAddress(seller)
            val originAddress = safeAddress(origin)

            if (!ensureSameBlockchain(
                    fullItemId.blockchain.group(),
                    sellerAddress?.blockchainGroup,
                    originAddress?.blockchainGroup
                )
            ) {
                logger.warn(
                    "Incompatible blockchain groups specified in getAuctionsByItem: itemId={}, origin={}, seller={}",
                    fullItemId.fullId(), origin, seller
                )
                return ResponseEntity.ok(empty)
            }

            val slice = router.getService(fullItemId.blockchain).getAuctionsByItem(
                fullItemId.value,
                sellerAddress?.value,
                sort,
                originAddress?.value,
                status,
                null,
                platform,
                continuation,
                size
            )
            return ResponseEntity.ok(toDto(slice))
        }

        override suspend fun getAuctionsBySeller(
            seller: String,
            blockchains: List<BlockchainDto>?,
            status: List<AuctionStatusDto>?,
            origin: String?,
            platform: PlatformDto?,
            continuation: String?,
            size: Int?
        ): ResponseEntity<AuctionsDto> {
            val sellerAddress = IdParser.parseAddress(seller)
            val originAddress = safeAddress(origin)
            val safeSize = PageSize.AUCTION.limit(size)
            val filter = BlockchainFilter(blockchains)
            if (!ensureSameBlockchain(sellerAddress.blockchainGroup, originAddress?.blockchainGroup)) {
                logger.warn(
                    "Incompatible blockchain groups specified in getAuctionsBySeller: origin={}, seller={}",
                    origin, seller
                )
                return ResponseEntity.ok(empty)
            }

            val blockchainSlices = router.executeForAll(filter.exclude(sellerAddress.blockchainGroup)) {
                it.getAuctionsBySeller(
                    sellerAddress.value,
                    status,
                    originAddress?.value,
                    platform,
                    continuation,
                    size
                )
            }

            val combinedSlice = Paging(
                AuctionContinuation.ByLastUpdateDesc,
                blockchainSlices.flatMap { it.entities }
            ).getSlice(safeSize)

            logger.info(
                "Response for getAuctionsBySeller" +
                        "(seller={}, status={}, origin={}, platform={}, continuation={}, size={}): " +
                        "Slice(size={}, continuation={})",
                seller, status, origin, platform, continuation, size,
                combinedSlice.entities.size, combinedSlice.continuation
            )

            return ResponseEntity.ok(toDto(combinedSlice))
        }

        private fun safeAddress(id: String?): UnionAddress? {
            return if (id == null) null else IdParser.parseAddress(id)
        }

        private fun toDto(slice: Slice<AuctionDto>): AuctionsDto {
            return AuctionsDto(
                total = slice.entities.size.toLong(),
                continuation = slice.continuation,
                auctions = slice.entities
            )
        }

        private fun toDto(slice: Slice<AuctionBidDto>): AuctionBidsDto {
            return AuctionBidsDto(
                total = slice.entities.size.toLong(),
                continuation = slice.continuation,
                bids = slice.entities
            )
        }

        private fun ensureSameBlockchain(vararg blockchains: BlockchainGroupDto?): Boolean {
            val set = blockchains.filterNotNull().toSet()
            return set.size == 1
        }
        */
}
