package com.rarible.protocol.union.core.converter

import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.core.model.EsTrait
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ext
import org.apache.commons.codec.digest.DigestUtils

object EsItemConverter {

   const val MAX_TRAIT_LENGTH = 3000

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
            bestSellCurrency = getCurrencyAddressOrNull(blockchain, this.bestSellOrder?.take),
            bestSellMarketplace = this.bestSellOrder?.platform?.name, // getting marketplace may be more complicated
            bestBidAmount = this.bestBidOrder?.make?.value?.toDouble(),
            bestBidCurrency = getCurrencyAddressOrNull(blockchain, this.bestBidOrder?.make),
            bestBidMarketplace = this.bestBidOrder?.platform?.name, // getting marketplace may be more complicated
        )
    }

    private fun getCurrencyAddressOrNull(blockchain: BlockchainDto, asset: AssetDto?): String? {
        val address = kotlin.runCatching {
            asset?.type?.ext?.currencyAddress()
        }.getOrNull() ?: return null
        return blockchain.name + ":" + address
    }

    private fun prepareId(itemId: String): String {
        return DigestUtils.sha256Hex(itemId)
    }
}
