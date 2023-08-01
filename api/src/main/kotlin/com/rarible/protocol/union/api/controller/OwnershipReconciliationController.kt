package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.service.select.OwnershipSourceSelectService
import com.rarible.protocol.union.core.exception.UnionException
import com.rarible.protocol.union.dto.OwnershipsDto
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
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

        val (from, id) = if (continuation?.contains("_") == true) {
            val dateIdContinuation = DateIdContinuation.parse(continuation)
            dateIdContinuation?.date to dateIdContinuation?.id
        } else {
            null to continuation
        }
        val ids = ownershipRepository.findIdsByLastUpdatedAt(
            lastUpdatedFrom = from ?: lastUpdatedFrom,
            lastUpdatedTo = lastUpdatedTo,
            fromId = id?.let { ShortOwnershipId(OwnershipIdParser.parseFull(id)) },
            size = size
        )
        if (ids.isEmpty()) {
            return OwnershipsDto()
        }
        val ownerships = ownershipSourceSelectService.getOwnershipsByIds(ids.map { it.id.toDto() })
        val next = if (ids.size == size) {
            val last = ids.last()
            DateIdContinuation(last.lastUpdatedAt, last.id.toDto().fullId())
        } else {
            null
        }
        return OwnershipsDto(
            total = 0,
            ownerships = ownerships,
            continuation = next?.toString()
        )
    }
}
