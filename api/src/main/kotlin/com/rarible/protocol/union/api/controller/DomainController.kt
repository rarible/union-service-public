package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.core.service.DomainService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.DomainResolveResultDto
import com.rarible.protocol.union.enrichment.converter.ResolveDomainResultDtoConverter
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class DomainController(
    private val router: BlockchainRouter<DomainService>
) : DomainControllerApi {

    override suspend fun resolve(domain: String): ResponseEntity<DomainResolveResultDto> {
        val result = router.getService(BlockchainDto.ETHEREUM).resolve(domain)
        return ResponseEntity.ok(ResolveDomainResultDtoConverter.convert(result))
    }
}
