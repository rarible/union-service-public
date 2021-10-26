package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.service.OwnershipApiService
import com.rarible.protocol.union.core.continuation.OwnershipContinuation
import com.rarible.protocol.union.core.continuation.page.PageSize
import com.rarible.protocol.union.core.continuation.page.Paging
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipsDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.dto.parser.OwnershipIdParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@ExperimentalCoroutinesApi
@RestController
class OwnershipController(
    private val ownershipApiService: OwnershipApiService,
    private val router: BlockchainRouter<OwnershipService>
) : OwnershipControllerApi {

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
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OwnershipsDto> {
        val safeSize = PageSize.OWNERSHIP.limit(size)
        val contractAddress = IdParser.parseAddress(contract)
        val result = router.getService(contractAddress.blockchain)
            .getOwnershipsByItem(contractAddress.value, tokenId, continuation, safeSize)

        val enriched = ownershipApiService.enrich(result)

        return ResponseEntity.ok(enriched)
    }

}
