package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.service.select.OwnershipSourceSelectService
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
        @RequestParam lastUpdatedFrom: Long,
        @RequestParam lastUpdatedTo: Long,
        @RequestParam(required = false) continuation: String? = null,
    ): OwnershipsDto {
        val ids = ownershipRepository.findIdsByLastUpdatedAt(
            lastUpdatedFrom = Instant.ofEpochMilli(lastUpdatedFrom),
            lastUpdatedTo = Instant.ofEpochMilli(lastUpdatedTo),
            continuation = continuation?.let { ShortOwnershipId(OwnershipIdParser.parseFull(continuation)) }
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