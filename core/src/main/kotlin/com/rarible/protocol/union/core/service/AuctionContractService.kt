package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class AuctionContractService(
    auctionServiceRouter: BlockchainRouter<AuctionService>
) {

    private val logger = LoggerFactory.getLogger(AuctionContractService::class.java)

    private val auctionContracts = auctionServiceRouter.getEnabledBlockchains().map {
        it to auctionServiceRouter.getService(it).getAuctionContracts()
    }.associateBy({ it.first }, { it.second })

    @PostConstruct
    fun log() {
        auctionContracts.forEach {
            logger.info("Found configured Auction contracts: {} : {}", it.key, it.value)
        }
    }

    suspend fun isAuctionContract(blockchain: BlockchainDto, contract: String): Boolean {
        return auctionContracts[blockchain]!!.contains(contract)
    }
}
