package com.rarible.protocol.union.core.service.dummy

import com.rarible.protocol.union.core.model.UnionDomainResolveResult
import com.rarible.protocol.union.core.service.DomainService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto

class DummyDomainService(
    blockchain: BlockchainDto
) : AbstractBlockchainService(blockchain), DomainService {

    override suspend fun resolve(name: String): UnionDomainResolveResult {
        return UnionDomainResolveResult(blockchain, "")
    }
}