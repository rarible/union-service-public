package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.enrichment.model.ShortOrder
import java.math.BigDecimal

object ShortOrderConverter {

    fun convert(order: OrderDto): ShortOrder {

        return ShortOrder(
            blockchain = order.id.blockchain,
            id = order.id.value,
            platform = order.platform.name,
            // We expect here orders with integer value of makeStock since there should be only NFTs
            makeStock =  order.makeStock.toBigInteger(),

            makePrice = if (isNft(order.make.type)) calculatePrice(order.make.value, order.take.value) else null,
            takePrice = if (isNft(order.take.type)) calculatePrice(order.take.value, order.make.value) else null,

            makePriceUsd = order.makePriceUsd,
            takePriceUsd = order.takePriceUsd
        )
    }

    private fun calculatePrice(payment: BigDecimal, nft: BigDecimal): BigDecimal? {
        return if (nft != BigDecimal.ZERO) payment / nft else null
    }

    private fun isNft(assetTypeDto: AssetTypeDto): Boolean {
        return when (assetTypeDto) {
            is EthCryptoPunksAssetTypeDto,
            is EthErc1155AssetTypeDto,
            is EthErc1155LazyAssetTypeDto,
            is EthErc721AssetTypeDto,
            is EthErc721LazyAssetTypeDto,
            is EthGenerativeArtAssetTypeDto,
            is FlowAssetTypeNftDto -> true

            is EthErc20AssetTypeDto,
            is EthEthereumAssetTypeDto,
            is FlowAssetTypeFtDto -> false
        }
    }
}
