package com.rarible.protocol.union.api.handler

import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.EthCollectionAssetTypeDto
import com.rarible.protocol.union.dto.EthErc1155AssetTypeDto
import com.rarible.protocol.union.dto.EthErc1155LazyAssetTypeDto
import com.rarible.protocol.union.dto.EthErc721AssetTypeDto
import com.rarible.protocol.union.dto.EthErc721LazyAssetTypeDto
import com.rarible.protocol.union.dto.EthGenerativeArtAssetTypeDto
import com.rarible.protocol.union.dto.FlowAssetTypeNftDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemSubscriptionEventDto
import com.rarible.protocol.union.dto.ItemSubscriptionRequestDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderSubscriptionEventDto
import com.rarible.protocol.union.dto.OrderTypeDto
import com.rarible.protocol.union.dto.OrderUpdateEventDto
import com.rarible.protocol.union.dto.OrdersByCollectionSubscriptionRequestDto
import com.rarible.protocol.union.dto.OrdersByItemSubscriptionRequestDto
import com.rarible.protocol.union.dto.OrdersSubscriptionRequestDto
import com.rarible.protocol.union.dto.OwnershipSubscriptionEventDto
import com.rarible.protocol.union.dto.OwnershipSubscriptionRequestDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.SolanaNftAssetTypeDto
import com.rarible.protocol.union.dto.SubscriptionActionDto
import com.rarible.protocol.union.dto.SubscriptionEventDto
import com.rarible.protocol.union.dto.SubscriptionRequestDto
import com.rarible.protocol.union.dto.TezosMTAssetTypeDto
import com.rarible.protocol.union.dto.TezosNFTAssetTypeDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

@Component
class SubscriptionHandler(
    private val itemUpdateListener: UnionSubscribeItemEventHandler,
    private val ownershipUpdateListener: UnionSubscribeOwnershipEventHandler,
    private val orderUpdateListener: UnionSubscribeOrderEventHandler,
) {

    fun handle(
        receive: () -> Flux<List<SubscriptionRequestDto>>,
        send: (Flux<SubscriptionEventDto>) -> Mono<Void>
    ): Mono<Void> {
        val itemSubscriptions = ConcurrentHashMap<Any, Set<Subscription<Always>>>()
        val ownershipsSubscriptions = ConcurrentHashMap<Any, Set<Subscription<Always>>>()
        val orderSubscriptions = ConcurrentHashMap<Any, Set<Subscription<OrderData>>>()

        return Mono.`when`(
            receive().doOnNext { message ->
                try {
                    for (request in message) {
                        when (request) {
                            is ItemSubscriptionRequestDto -> Subscription.handleSubscriptionRequest(
                                request.action == SubscriptionActionDto.SUBSCRIBE,
                                request.id,
                                itemSubscriptions,
                                AlwaysSubscription
                            )

                            is OwnershipSubscriptionRequestDto -> Subscription.handleSubscriptionRequest(
                                request.action == SubscriptionActionDto.SUBSCRIBE,
                                request.id,
                                ownershipsSubscriptions,
                                AlwaysSubscription
                            )

                            is OrdersSubscriptionRequestDto -> Subscription.handleSubscriptionRequest(
                                request.action == SubscriptionActionDto.SUBSCRIBE,
                                Root,
                                orderSubscriptions,
                                OrderSubscription(request.orderType, request.platform),
                            )

                            is OrdersByCollectionSubscriptionRequestDto -> Subscription.handleSubscriptionRequest(
                                request.action == SubscriptionActionDto.SUBSCRIBE,
                                request.collectionId,
                                orderSubscriptions,
                                OrderSubscription(request.orderType, request.platform),
                            )

                            is OrdersByItemSubscriptionRequestDto -> Subscription.handleSubscriptionRequest(
                                request.action == SubscriptionActionDto.SUBSCRIBE,
                                request.itemId,
                                orderSubscriptions,
                                OrderSubscription(request.orderType, request.platform),
                            )
                        }
                    }
                } catch (ex: Throwable) {
                    logger.error("Unable to read message", ex)
                }
            },
            send(
                Flux.merge(
                    itemUpdateListener.updates
                        .filter { Subscription.isSubscribed(itemSubscriptions, Always, it.itemId) }
                        .map { ItemSubscriptionEventDto(it) },

                    ownershipUpdateListener.updates
                        .filter { Subscription.isSubscribed(ownershipsSubscriptions, Always, it.ownershipId) }
                        .map { OwnershipSubscriptionEventDto(it) },

                    orderUpdateListener.updates
                        .filter {
                            when (it) {
                                is OrderUpdateEventDto -> {
                                    val data = extractOrderData(it.order)
                                    if (data != null) {
                                        Subscription.isSubscribed(
                                            orderSubscriptions,
                                            data,
                                            Root,
                                            data.itemId,
                                            data.collectionId
                                        )
                                    } else {
                                        false
                                    }
                                }

                                else -> {
                                    logger.error("Unable to process OrderEvent: $it")
                                    false
                                }

                            }
                        }
                        .map { OrderSubscriptionEventDto(it) },
                    )
            )
        )
    }

    private fun extractOrderData(order: OrderDto): OrderData? {
        val makeItemId = extractItemId(order.make.type)
        val makeCollectionId = extractCollectionId(order.make.type)
        if (makeItemId != null || makeCollectionId != null) {
            return OrderData(makeItemId, makeCollectionId, order.platform, OrderTypeDto.SELL)
        }

        val takeItemId = extractItemId(order.make.type)
        val takeCollectionId = extractCollectionId(order.make.type)
        if (takeItemId != null || takeCollectionId != null) {
            return OrderData(takeItemId, takeCollectionId, order.platform, OrderTypeDto.BID)
        }

        return null
    }

    private fun extractItemId(assetType: AssetTypeDto): ItemIdDto? {
        return when (assetType) {
            is EthErc721AssetTypeDto -> ItemIdDto(
                assetType.contract.blockchain,
                assetType.contract.value,
                assetType.tokenId
            )

            is EthErc721LazyAssetTypeDto -> ItemIdDto(
                assetType.contract.blockchain,
                assetType.contract.value,
                assetType.tokenId
            )

            is EthErc1155AssetTypeDto -> ItemIdDto(
                assetType.contract.blockchain,
                assetType.contract.value,
                assetType.tokenId
            )

            is EthErc1155LazyAssetTypeDto -> ItemIdDto(
                assetType.contract.blockchain,
                assetType.contract.value,
                assetType.tokenId
            )

            is TezosMTAssetTypeDto -> ItemIdDto(
                assetType.contract.blockchain,
                assetType.contract.value,
                assetType.tokenId
            )

            is TezosNFTAssetTypeDto -> ItemIdDto(
                assetType.contract.blockchain,
                assetType.contract.value,
                assetType.tokenId
            )

            is SolanaNftAssetTypeDto -> assetType.itemId

            is FlowAssetTypeNftDto -> ItemIdDto(
                assetType.contract.blockchain,
                assetType.contract.value,
                assetType.tokenId
            )

            else -> null
        }
    }

    private fun extractCollectionId(assetType: AssetTypeDto): CollectionIdDto? {
        return when (assetType) {
            //TODO we assume that Contract = Collection for EVM based NFTs
            is EthCollectionAssetTypeDto -> CollectionIdDto(
                assetType.contract.blockchain,
                assetType.contract.value
            )

            is EthGenerativeArtAssetTypeDto -> CollectionIdDto(
                assetType.contract.blockchain,
                assetType.contract.value,
            )

            is EthErc721AssetTypeDto -> CollectionIdDto(
                assetType.contract.blockchain,
                assetType.contract.value
            )

            is EthErc721LazyAssetTypeDto -> CollectionIdDto(
                assetType.contract.blockchain,
                assetType.contract.value,
            )

            is EthErc1155AssetTypeDto -> CollectionIdDto(
                assetType.contract.blockchain,
                assetType.contract.value
            )

            is EthErc1155LazyAssetTypeDto -> CollectionIdDto(
                assetType.contract.blockchain,
                assetType.contract.value
            )

            is TezosMTAssetTypeDto -> CollectionIdDto(
                assetType.contract.blockchain,
                assetType.contract.value
            )

            is TezosNFTAssetTypeDto -> CollectionIdDto(
                assetType.contract.blockchain,
                assetType.contract.value
            )

            //TODO we can't watch for "byCollection" subscriptions on Solana (NOW)
            is SolanaNftAssetTypeDto -> null

            is FlowAssetTypeNftDto -> CollectionIdDto(
                assetType.contract.blockchain,
                assetType.contract.value
            )

            else -> null
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(SubscriptionHandler::class.java)
    }
}

object Root
object Always

object AlwaysSubscription : Subscription<Always>() {
    override fun covers(event: Always): Boolean = true
}

data class OrderData(
    val itemId: ItemIdDto?,
    val collectionId: CollectionIdDto?,
    val platform: PlatformDto,
    val orderTypeDto: OrderTypeDto,
)

data class OrderSubscription(
    val type: OrderTypeDto?,
    val platform: PlatformDto?,
) : Subscription<OrderData>() {
    override fun covers(event: OrderData): Boolean {
        if (type != null && event.orderTypeDto != type) return false
        if (platform != null && event.platform != platform) return false
        return true
    }
}