package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.dipdup.client.RoyaltiesClient
import com.rarible.dipdup.client.core.model.Part
import com.rarible.dipdup.client.exception.DipDupNotFound
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupItemConverter
import org.slf4j.LoggerFactory

class DipDupRoyaltyService(
    private val dipDupRoyaltyClient: RoyaltiesClient
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getItemRoyaltiesById(itemId: String): List<RoyaltyDto> {
        return try {
            val royalties = dipDupRoyaltyClient.getRoyaltiesById(itemId)
            DipDupItemConverter.convert(royalties)
        } catch (ex: DipDupNotFound) {
            logger.debug("Royalty wasn't found")
            emptyList()
        }
    }

    suspend fun saveRoyalty(itemId: String, royalties: List<RoyaltyDto>) {
        val parts = royalties.map { Part(it.account.value, it.value) }
        dipDupRoyaltyClient.insertRoyalty(itemId, parts)
    }
}
