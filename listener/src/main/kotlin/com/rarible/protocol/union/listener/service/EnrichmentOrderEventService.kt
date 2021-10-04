package com.rarible.protocol.union.listener.service

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.EthCryptoPunksAssetTypeDto
import com.rarible.protocol.union.dto.EthErc1155AssetTypeDto
import com.rarible.protocol.union.dto.EthErc1155LazyAssetTypeDto
import com.rarible.protocol.union.dto.EthErc20AssetTypeDto
import com.rarible.protocol.union.dto.EthErc721AssetTypeDto
import com.rarible.protocol.union.dto.EthErc721LazyAssetTypeDto
import com.rarible.protocol.union.dto.EthEthereumAssetTypeDto
import com.rarible.protocol.union.dto.EthGenerativeArtAssetTypeDto
import com.rarible.protocol.union.dto.FlowAssetTypeFtDto
import com.rarible.protocol.union.dto.FlowAssetTypeNftDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigInteger

@Component
class EnrichmentOrderEventService(
    private val enrichmentItemEventService: EnrichmentItemEventService,
    private val enrichmentOwnershipEventService: EnrichmentOwnershipEventService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun updateOrder(order: OrderDto) = coroutineScope {
        val makeItemId = toItemId(order.make.type)
        val takeItemId = toItemId(order.take.type)

        val mFuture = makeItemId?.let {
            async { ignoreApi404 { enrichmentItemEventService.onItemBestSellOrderUpdated(makeItemId, order) } }
        }
        val tFuture = takeItemId?.let {
            async { ignoreApi404 { enrichmentItemEventService.onItemBestBidOrderUpdated(takeItemId, order) } }
        }
        val oFuture = makeItemId?.let {
            val ownershipId = ShortOwnershipId(
                makeItemId.blockchain,
                makeItemId.token,
                makeItemId.tokenId,
                order.maker.value
            )
            async {
                ignoreApi404 {
                    enrichmentOwnershipEventService.onOwnershipBestSellOrderUpdated(
                        ownershipId,
                        order
                    )
                }
            }
        }

        mFuture?.await()
        tFuture?.await()
        oFuture?.await()
    }

    private fun toItemId(assetType: AssetTypeDto): ShortItemId? {

        return when (assetType) {
            // Ethereum
            is EthErc721AssetTypeDto -> toItemId(assetType.contract, assetType.tokenId)
            is EthErc1155AssetTypeDto -> toItemId(assetType.contract, assetType.tokenId)
            is EthErc721LazyAssetTypeDto -> toItemId(assetType.contract, assetType.tokenId)
            is EthErc1155LazyAssetTypeDto -> toItemId(assetType.contract, assetType.tokenId)
            is EthCryptoPunksAssetTypeDto -> toItemId(assetType.contract, assetType.punkId.toBigInteger())
            is EthGenerativeArtAssetTypeDto -> null
            is EthEthereumAssetTypeDto -> null
            is EthErc20AssetTypeDto -> null

            // Flow
            is FlowAssetTypeNftDto -> toItemId(assetType.contract, assetType.tokenId)
            is FlowAssetTypeFtDto -> null
        }
    }

    private fun toItemId(contract: UnionAddress, tokenId: BigInteger): ShortItemId {
        return ShortItemId(
            contract.blockchain,
            contract.value,
            tokenId
        )
    }

    private suspend fun ignoreApi404(call: suspend () -> Unit) {
        try {
            call()
        } catch (ex: WebClientResponseProxyException) {
            logger.warn("Received NOT_FOUND code from client, details: {}, message: {}", ex.data, ex.message)
        }
    }

}