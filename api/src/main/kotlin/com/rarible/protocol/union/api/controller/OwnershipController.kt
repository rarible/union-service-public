package com.rarible.protocol.union.api.controller

import com.rarible.core.logging.Logger
import com.rarible.protocol.union.api.service.select.OwnershipSourceSelectService
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipsDto
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.dto.parser.OwnershipIdParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class OwnershipController(
    private val ownershipSourceSelectService: OwnershipSourceSelectService,
) : OwnershipControllerApi {

    companion object {
        private val logger by Logger()
    }

    override suspend fun getOwnershipById(
        ownershipId: String,
    ): ResponseEntity<OwnershipDto> {
        val fullOwnershipId = OwnershipIdParser.parseFull(ownershipId)

        val ownership = ownershipSourceSelectService.getOwnershipById(fullOwnershipId)

        return ResponseEntity.ok(ownership)
    }

    override suspend fun getOwnershipsByItem(
        itemId: String,
        continuation: String?,
        size: Int?
    ): ResponseEntity<OwnershipsDto> {
        val safeSize = PageSize.OWNERSHIP.limit(size)
        val fullItemId = IdParser.parseItemId(itemId)
        val result = ownershipSourceSelectService.getOwnershipsByItem(fullItemId, continuation, safeSize)

        logger.info(
            "Response for getOwnershipsByItem(itemId={}, continuation={}, size={}):" +
                " Slice(size={}, continuation={}) ",
            fullItemId.fullId(), continuation, size, result.ownerships.size, result.continuation
        )

        return ResponseEntity.ok(result)
    }
}
