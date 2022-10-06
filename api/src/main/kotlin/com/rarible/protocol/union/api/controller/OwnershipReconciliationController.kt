package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.service.select.OwnershipSourceSelectService
import com.rarible.protocol.union.core.exception.UnionException
import com.rarible.protocol.union.dto.OwnershipsDto
import com.rarible.protocol.union.dto.parser.OwnershipIdParser
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.repository.OwnershipRepository
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class OwnershipReconciliationController(
    private val ownershipSourceSelectService: OwnershipSourceSelectService,
    private val ownershipRepository: OwnershipRepository,
) {

    @GetMapping(value = ["/reconciliation/ownerships"], produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun getOwnerships(
        @RequestParam lastUpdatedFrom: Instant,
        @RequestParam lastUpdatedTo: Instant,
        @RequestParam(required = false) continuation: String? = null,
        @RequestParam(required = false, defaultValue = "20") size: Int,
    ): OwnershipsDto {
        if (size !in 1..200) throw UnionException("Size param must be between 1 and 200")

        val ids = ownershipRepository.findIdsByLastUpdatedAt(
            lastUpdatedFrom = lastUpdatedFrom,
            lastUpdatedTo = lastUpdatedTo,
            continuation = continuation?.let { ShortOwnershipId(OwnershipIdParser.parseFull(continuation)) },
            size = size
        ).map { it.toDto() }
        if (ids.isEmpty()) {
            return OwnershipsDto()
        }

        val ownerships = ownershipSourceSelectService.getOwnershipsByIds(ids)

        return OwnershipsDto(
            total = 0,
            ownerships = ownerships,
            continuation = ids.last().fullId()
        )
    }
}
