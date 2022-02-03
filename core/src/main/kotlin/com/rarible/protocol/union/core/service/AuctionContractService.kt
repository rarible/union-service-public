package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.core.DefaultBlockchainProperties
import com.rarible.protocol.union.dto.BlockchainDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class AuctionContractService(
    blockchainProperties: List<DefaultBlockchainProperties>
) {

    private val logger = LoggerFactory.getLogger(AuctionContractService::class.java)

    private val auctionContracts = blockchainProperties.associateBy({ it.blockchain }, {
        val parsedContracts = it.auctionContracts?.split(",")?.filter { str -> str.isNotBlank() } ?: emptyList()
        HashSet(parsedContracts)
    })


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