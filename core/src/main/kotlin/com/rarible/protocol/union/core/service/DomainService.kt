package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.core.model.UnionDomainResolveResult
import com.rarible.protocol.union.core.service.router.BlockchainService

interface DomainService : BlockchainService {

    suspend fun resolve(name: String): UnionDomainResolveResult
}
