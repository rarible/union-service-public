package com.rarible.protocol.union.core.service.router

import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.stereotype.Component

@Component
class ActiveBlockchainProvider(
    activeBlockchains: List<ActiveBlockchain>
) {

    companion object {
        // Convenient constructor for tests
        fun of(vararg blockchains: BlockchainDto) =
            ActiveBlockchainProvider(listOf(ActiveBlockchain(blockchains.toList())))
    }

    val blockchains = build(activeBlockchains)

    private fun build(activeBlockchains: List<ActiveBlockchain>): Set<BlockchainDto> {
        val all = activeBlockchains.flatMap { it.active }
            .groupBy { it }

        all.values.find { it.size > 1 }?.let {
            throw IllegalStateException("Blockchain ${it.first()} specified as active ${it.size} times")
        }

        return HashSet(all.keys)
    }
}
