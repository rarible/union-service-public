package com.rarible.protocol.union.core.converter

import com.rarible.protocol.union.core.converter.helper.getCurrencyIdOrNull
import com.rarible.protocol.union.core.model.elastic.EsItem
import com.rarible.protocol.union.core.model.elastic.EsItemTrait
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import org.apache.commons.codec.digest.DigestUtils

object EsItemConverter {

    private const val MAX_TRAIT_LENGTH = 1000

    fun ItemDto.toEsItem(): EsItem {
        return EsItem(
            id = prepareId(id.fullId()),
            itemId = id.fullId(),
            blockchain = blockchain,
            collection = collection?.fullId(),
            token = collection?.value,
            tokenId = tokenId(id),
            name = meta?.name,
            description = meta?.description,
            creators = creators.map { it.account.fullId() },
            mintedAt = mintedAt,
            lastUpdatedAt = lastUpdatedAt,
            deleted = deleted,
            traits = meta?.attributes?.map { EsItemTrait(it.key.take(MAX_TRAIT_LENGTH), it.value?.take(MAX_TRAIT_LENGTH)) }
                ?: emptyList(),
            self = self,
            bestSellAmount = this.bestSellOrder?.take?.value?.toDouble(),
            bestSellCurrency = getCurrencyIdOrNull(blockchain, this.bestSellOrder?.take),
            bestSellMarketplace = this.bestSellOrder?.platform?.name, // getting marketplace may be more complicated
            bestSellCreatedAt = this.bestSellOrder?.createdAt,
            bestBidAmount = this.bestBidOrder?.make?.value?.toDouble(),
            bestBidCurrency = getCurrencyIdOrNull(blockchain, this.bestBidOrder?.make),
            bestBidMarketplace = this.bestBidOrder?.platform?.name, // getting marketplace may be more complicated
        )
    }

    private fun tokenId(itemId: ItemIdDto): String? {
        if (itemId.blockchain == BlockchainDto.SOLANA) {
            return itemId.value
        }
        val parts = itemId.value.split(":")
        return if (parts.size == 2) parts[1] else itemId.value
    }

    private fun prepareId(itemId: String): String {
        return DigestUtils.sha256Hex(itemId)
    }
}
