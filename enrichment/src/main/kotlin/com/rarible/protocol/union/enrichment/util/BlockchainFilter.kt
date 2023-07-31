package com.rarible.protocol.union.enrichment.util

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.subchains

class BlockchainFilter(
    fromQuery: Collection<BlockchainDto>?
) {

    private val filter = fromQuery?.let { HashSet(it) }

    fun exclude(group: BlockchainGroupDto): List<BlockchainDto> {
        return exclude(group.subchains())
    }

    fun exclude(group: Set<BlockchainGroupDto>): List<BlockchainDto> {
        return exclude(group.flatMap { it.subchains() })
    }

    fun exclude(blockchains: List<BlockchainDto>): List<BlockchainDto> {
        if (filter == null || filter.isEmpty()) {
            return blockchains
        }
        return blockchains.filter { filter.contains(it) }
    }
}
