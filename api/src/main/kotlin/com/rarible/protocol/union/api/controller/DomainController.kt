package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.service.domain.DomainResolveService
import com.rarible.protocol.union.dto.DomainResolveResultDto
import com.rarible.protocol.union.enrichment.converter.ResolveDomainResultDtoConverter
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class DomainController(
    private val domainResolveService: DomainResolveService
) : DomainControllerApi {

    override suspend fun resolve(domain: String): ResponseEntity<DomainResolveResultDto> {
        val result = domainResolveService.resolve(domain)
        return ResponseEntity.ok(ResolveDomainResultDtoConverter.convert(result))
    }
}
