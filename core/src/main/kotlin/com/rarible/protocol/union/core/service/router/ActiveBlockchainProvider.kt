package com.rarible.protocol.union.core.service.router

import com.rarible.protocol.union.dto.BlockchainDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ActiveBlockchainProvider(
    activeBlockchains: List<ActiveBlockchain>
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        // Convenient constructor for tests
        fun of(vararg blockchains: BlockchainDto) =
            ActiveBlockchainProvider(listOf(ActiveBlockchain(blockchains.toList())))
    }

    val blockchains = build(activeBlockchains)

    fun isActive(blockchain: BlockchainDto) = blockchains.contains(blockchain)

    private fun build(activeBlockchains: List<ActiveBlockchain>): Set<BlockchainDto> {
        val all = activeBlockchains.flatMap { it.active }
            .groupBy { it }

        all.values.find { it.size > 1 }?.let {
            throw IllegalStateException("Blockchain ${it.first()} specified as active ${it.size} times")
        }

        logger.info("Active Blockchains: {}", all.keys)
        return HashSet(all.keys)
    }
}
