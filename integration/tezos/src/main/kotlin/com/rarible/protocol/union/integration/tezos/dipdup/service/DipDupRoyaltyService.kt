package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.dipdup.client.RoyaltiesClient
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupItemConverter

class DipDupRoyaltyService(
    private val dipDupRoyaltyClient: RoyaltiesClient
) {

    suspend fun getItemRoyaltiesById(itemId: String): List<RoyaltyDto> {
        val royalties = dipDupRoyaltyClient.getRoyaltiesById(itemId)
        return DipDupItemConverter.convert(royalties)
    }
}
