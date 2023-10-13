package com.rarible.protocol.union.integration.ethereum.mock

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.EthBlockchainClients
import com.rarible.protocol.union.integration.ethereum.EthClients
import io.mockk.mockk

object EthClientsMock {

    fun testEthClients(blockchains: List<BlockchainDto>): EthClients {
        return blockchains.map {
            it to EthBlockchainClients(
                balanceControllerApi = mockk(),

                nftItemControllerApi = mockk(),
                nftLazyMintControllerApi = mockk(),
                nftOwnershipControllerApi = mockk(),
                nftCollectionControllerApi = mockk(),
                nftActivityControllerApi = mockk(),
                nftDomainControllerApi = mockk(),

                orderActivityControllerApi = mockk(),
                orderControllerApi = mockk(),
                auctionControllerApi = mockk(),
                orderSignatureControllerApi = mockk(),
                orderAdminControllerApi = mockk(),
                auctionActivityControllerApi = mockk(),
            )
        }.associateBy({ it.first }, { it.second }).let { EthClients(it) }
    }
}
