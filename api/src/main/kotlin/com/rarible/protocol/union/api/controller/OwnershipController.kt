package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.configuration.PageSize
import com.rarible.protocol.union.core.continuation.ContinuationPaging
import com.rarible.protocol.union.core.service.OwnershipServiceRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.IdParser
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipsDto
import com.rarible.protocol.union.dto.continuation.OwnershipContinuation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class OwnershipController(
    private val router: OwnershipServiceRouter
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

        val combinedPage = ContinuationPaging(
            OwnershipContinuation.ByLastUpdatedAndId,
            blockchainPages.flatMap { it.ownerships }
        ).getPage(safeSize)

        val result = OwnershipsDto(total, combinedPage.printContinuation(), combinedPage.entities)
        return ResponseEntity.ok(result)
    }

    override suspend fun getOwnershipById(
        ownershipId: String
    ): ResponseEntity<OwnershipDto> {
        val (blockchain, shortOwnershipId) = IdParser.parse(ownershipId)
        val result = router.getService(blockchain).getOwnershipById(shortOwnershipId)
        return ResponseEntity.ok(result)
    }

    override suspend fun getOwnershipsByItem(
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OwnershipsDto> {
        val safeSize = PageSize.OWNERSHIP.limit(size)
        val (blockchain, shortContract) = IdParser.parse(contract)
        val result = router.getService(blockchain).getOwnershipsByItem(shortContract, tokenId, continuation, safeSize)
        return ResponseEntity.ok(result)
    }
}
