package com.rarible.protocol.union.api.service.domain

import com.rarible.protocol.union.api.configuration.DomainProperties
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.exception.UnionValidationException
import com.rarible.protocol.union.core.model.UnionDomainResolveResult
import com.rarible.protocol.union.core.service.DomainService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import org.springframework.stereotype.Component

@Component
class DomainResolveService(
    private val router: BlockchainRouter<DomainService>,
    private val properties: DomainProperties,
) {
    suspend fun resolve(domain: String): UnionDomainResolveResult {
        val topDomain = TopLevelDomainExtractor.extract(domain)
            ?: throw UnionValidationException("Can't determine top level domain for $domain")
        val blockchain = properties.findBlockchain(topDomain)
            ?: throw UnionNotFoundException("Can't find blockchain for $domain")

        return router.getService(blockchain).resolve(domain)
    }
}
