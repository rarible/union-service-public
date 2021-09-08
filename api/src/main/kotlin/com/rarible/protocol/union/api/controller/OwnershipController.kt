package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.continuation.ContinuationPaging
import com.rarible.protocol.union.core.service.OwnershipServiceRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.IdParser
import com.rarible.protocol.union.dto.UnionOwnershipDto
import com.rarible.protocol.union.dto.UnionOwnershipsDto
import com.rarible.protocol.union.dto.continuation.UnionOwnershipContinuationFactory
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
    ): ResponseEntity<UnionOwnershipsDto> {
        val blockchainPages = router.executeForAll(blockchains) {
            it.getAllOwnerships(continuation, size)
        }

        val total = blockchainPages.map { it.total }.sum()

        val combinedPage = ContinuationPaging(
            UnionOwnershipContinuationFactory.ByLastUpdatedAndId,
            blockchainPages.flatMap { it.ownerships }
        ).getPage(size)

        val result = UnionOwnershipsDto(total, combinedPage.continuation.toString(), combinedPage.entities)
        return ResponseEntity.ok(result)
    }

    override suspend fun getOwnershipById(
        ownershipId: String
    ): ResponseEntity<UnionOwnershipDto> {
        val (blockchain, shortOwnershipId) = IdParser.parse(ownershipId)
        val result = router.getService(blockchain).getOwnershipById(shortOwnershipId)
        return ResponseEntity.ok(result)
    }

    override suspend fun getOwnershipsByItem(
        contract: String,
        tokenId: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<UnionOwnershipsDto> {
        val (blockchain, shortContract) = IdParser.parse(contract)
        val result = router.getService(blockchain).getOwnershipsByItem(shortContract, tokenId, continuation, size)
        return ResponseEntity.ok(result)
    }
}