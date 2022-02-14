package com.rarible.protocol.union.api.util

import com.rarible.protocol.union.core.FeatureFlags
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.subchains
import org.springframework.stereotype.Component

@Component
class BlockchainFilter(
    val featureFlags: FeatureFlags
) {

    fun getEnabledInApi(group: BlockchainGroupDto): List<BlockchainDto> {
        return getEnabledInApi(group.subchains())
    }

    fun getEnabledInApi(blockchains: List<BlockchainDto>): List<BlockchainDto> {
        val result = ArrayList(blockchains)
        if (!featureFlags.enablePolygonInApi) {
            result.remove(BlockchainDto.POLYGON)
        }
        return result
    }

}