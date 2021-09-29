package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.configuration.PageSize
import com.rarible.protocol.union.core.continuation.Page
import com.rarible.protocol.union.core.continuation.Paging
import com.rarible.protocol.union.core.service.OwnershipServiceRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.IdParser
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipsDto
import com.rarible.protocol.union.dto.UnionOwnershipDto
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

        val combinedPage = Paging(
            OwnershipContinuation.ByLastUpdatedAndId,
            blockchainPages.flatMap { it.entities }
        ).getPage(safeSize, total)

        val enriched = enrich(combinedPage)

        return ResponseEntity.ok(enriched)
    }

    override suspend fun getOwnershipById(
        ownershipId: String
    ): ResponseEntity<OwnershipDto> {
        val (blockchain, shortOwnershipId) = IdParser.parse(ownershipId)
        val result = router.getService(blockchain).getOwnershipById(shortOwnershipId)

        val enriched = enrich(result)

        return ResponseEntity.ok(enriched)
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

        val enriched = enrich(result)

        return ResponseEntity.ok(enriched)
    }

    private fun enrich(unionOwnershipsPage: Page<UnionOwnershipDto>): OwnershipsDto {
        return OwnershipsDto(
            total = unionOwnershipsPage.total,
            continuation = unionOwnershipsPage.continuation,
            ownerships = enrich(unionOwnershipsPage.entities)
        )
    }

    private fun enrich(unionItem: UnionOwnershipDto): OwnershipDto {
        // TODO
        throw NotImplementedError("IMPLEMENT ITEM ENRICHMENT!")
    }

    private fun enrich(unionItems: List<UnionOwnershipDto>): List<OwnershipDto> {
        // TODO
        throw NotImplementedError("IMPLEMENT ITEM ENRICHMENT!")
    }
}
