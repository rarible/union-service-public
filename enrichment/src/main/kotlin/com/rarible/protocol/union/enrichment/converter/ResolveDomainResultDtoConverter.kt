package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.model.UnionDomainResolveResult
import com.rarible.protocol.union.dto.DomainResolveResultDto

object ResolveDomainResultDtoConverter {
    fun convert(source: UnionDomainResolveResult): DomainResolveResultDto {
        return DomainResolveResultDto(
            source.blockchain,
            source.registrant
        )
    }
}
