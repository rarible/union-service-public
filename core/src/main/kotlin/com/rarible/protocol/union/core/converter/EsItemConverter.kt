package com.rarible.protocol.union.core.converter

import com.rarible.protocol.union.core.converter.helper.getCurrencyIdOrNull
import com.rarible.protocol.union.core.model.elastic.EsItem
import com.rarible.protocol.union.core.model.elastic.EsTrait
import com.rarible.protocol.union.dto.ItemDto
import org.apache.commons.codec.digest.DigestUtils

object EsItemConverter {

    private const val MAX_TRAIT_LENGTH = 1000

    fun ItemDto.toEsItem(): EsItem {
        return EsItem(
            id = prepareId(id.fullId()),
            itemId = id.fullId(),
            blockchain = blockchain,
            collection = collection?.fullId(),
            name = meta?.name,
            description = meta?.description,
            creators = creators.map { it.account.fullId() },
            mintedAt = mintedAt,
            lastUpdatedAt = lastUpdatedAt,
            deleted = deleted,
            traits = meta?.attributes?.map { EsTrait(it.key.take(MAX_TRAIT_LENGTH), it.value?.take(MAX_TRAIT_LENGTH)) } ?: emptyList(),
            self = self,
            bestSellAmount = this.bestSellOrder?.take?.value?.toDouble(),
            bestSellCurrency = getCurrencyIdOrNull(blockchain, this.bestSellOrder?.take),
            bestSellMarketplace = this.bestSellOrder?.platform?.name, // getting marketplace may be more complicated
            bestBidAmount = this.bestBidOrder?.make?.value?.toDouble(),
            bestBidCurrency = getCurrencyIdOrNull(blockchain, this.bestBidOrder?.make),
            bestBidMarketplace = this.bestBidOrder?.platform?.name, // getting marketplace may be more complicated
        )
    }

    private fun prepareId(itemId: String): String {
        return DigestUtils.sha256Hex(itemId)
    }
}
