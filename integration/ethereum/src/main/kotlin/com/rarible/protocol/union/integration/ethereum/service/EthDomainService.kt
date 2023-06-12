package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.api.client.NftDomainControllerApi
import com.rarible.protocol.union.core.model.UnionDomainResolveResult
import com.rarible.protocol.union.core.service.DomainService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import kotlinx.coroutines.reactive.awaitFirst

open class EthDomainService(
    blockchain: BlockchainDto,
    private val nftDomainControllerApi: NftDomainControllerApi
) : AbstractBlockchainService(blockchain), DomainService {

    override suspend fun resolve(name: String): UnionDomainResolveResult {
        val result = nftDomainControllerApi.resolveDomainByName(name).awaitFirst()
        return UnionDomainResolveResult(blockchain, result.registrant)
    }
}

@CaptureSpan(type = "blockchain")
open class EthereumDomainService(
    nftDomainControllerApi: NftDomainControllerApi
) : EthDomainService(
    BlockchainDto.ETHEREUM,
    nftDomainControllerApi
)

@CaptureSpan(type = "blockchain")
open class PolygonDomainService(
    nftDomainControllerApi: NftDomainControllerApi
) : EthDomainService(
    BlockchainDto.POLYGON,
    nftDomainControllerApi
)