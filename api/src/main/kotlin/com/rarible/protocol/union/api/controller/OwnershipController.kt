package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.service.OwnershipApiService
import com.rarible.protocol.union.api.service.extractItemId
import com.rarible.protocol.union.core.continuation.OwnershipContinuation
import com.rarible.protocol.union.core.continuation.page.PageSize
import com.rarible.protocol.union.core.continuation.page.Paging
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipsDto
import com.rarible.protocol.union.dto.parser.OwnershipIdParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@ExperimentalCoroutinesApi
@RestController
class OwnershipController(
    private val ownershipApiService: OwnershipApiService,
    private val router: BlockchainRouter<OwnershipService>
) : OwnershipControllerApi {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun getAllOwnerships(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OwnershipsDto> {
        val safeSize = PageSize.OWNERSHIP.limit(size)
        val blockchainPages = router.executeForAll(blockchains) {
            it.getAllOwnerships(continuation, safeSize)
        }

        val total = blockchainPages.map { it.total }.sum()

        val combinedPage = Paging(
            OwnershipContinuation.ByLastUpdatedAndId,
            blockchainPages.flatMap { it.entities }
        ).getPage(safeSize, total)

        logger.info("Response for getAllOwnerships(blockchains={}, continuation={}, size={}):" +
                " Page(size={}, total={}, continuation={}) from blockchain pages {} ",
            blockchains, continuation, size, combinedPage.entities.size, combinedPage.total,
            combinedPage.continuation, blockchainPages.map { it.entities.size }
        )

        val enriched = ownershipApiService.enrich(combinedPage)
        return ResponseEntity.ok(enriched)
    }

    override suspend fun getOwnershipById(
        ownershipId: String
    ): ResponseEntity<OwnershipDto> {
        val fullOwnershipId = OwnershipIdParser.parseFull(ownershipId)
        val result = router.getService(fullOwnershipId.blockchain).getOwnershipById(fullOwnershipId.value)

        val enriched = ownershipApiService.enrich(result)

        return ResponseEntity.ok(enriched)
    }

    override suspend fun getOwnershipsByItem(
        itemId: String?,
        contract: String?,
        tokenId: String?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OwnershipsDto> {
        val safeSize = PageSize.OWNERSHIP.limit(size)
        val fullItemId = extractItemId(contract, tokenId, itemId)
        val result = router.getService(fullItemId.blockchain)
            .getOwnershipsByItem(fullItemId.contract, fullItemId.tokenId.toString(), continuation, safeSize)

        logger.info(
            "Response for getOwnershipsByItem(itemId={}, continuation={}, size={}):" +
                    " Page(size={}, total={}, continuation={}) from blockchain pages {} ",
            fullItemId.fullId(), continuation, size, result.entities.size, result.total, result.continuation
        )

        val enriched = ownershipApiService.enrich(result)
        return ResponseEntity.ok(enriched)
    }

}
