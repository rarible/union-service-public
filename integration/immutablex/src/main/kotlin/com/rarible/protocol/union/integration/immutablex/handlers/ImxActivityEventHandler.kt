package com.rarible.protocol.union.integration.immutablex.handlers

import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.core.model.UnionOwnershipDeleteEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexDeposit
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexEvent
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexMint
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrder
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTrade
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTransfer
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexWithdrawal
import com.rarible.protocol.union.integration.immutablex.client.TokenIdDecoder
import com.rarible.protocol.union.integration.immutablex.converter.ImxActivityConverter
import com.rarible.protocol.union.integration.immutablex.converter.ImxDataException
import com.rarible.protocol.union.integration.immutablex.converter.ImxItemConverter
import com.rarible.protocol.union.integration.immutablex.converter.ImxOwnershipConverter
import com.rarible.protocol.union.integration.immutablex.scanner.ImxScanEntityType
import com.rarible.protocol.union.integration.immutablex.scanner.ImxScanMetrics
import com.rarible.protocol.union.integration.immutablex.service.ImxActivityService
import com.rarible.protocol.union.integration.immutablex.service.ImxItemService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import java.time.Instant

class ImxActivityEventHandler(
    private val activityHandler: IncomingEventHandler<ActivityDto>,
    private val itemHandler: IncomingEventHandler<UnionItemEvent>,
    private val ownershipHandler: IncomingEventHandler<UnionOwnershipEvent>,

    private val itemService: ImxItemService,
    private val activityService: ImxActivityService,

    private val imxScanMetrics: ImxScanMetrics,
) {

    private val blockchain = BlockchainDto.IMMUTABLEX

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun handle(events: List<ImmutablexEvent>) {

        // Orders needed to fulfill trades
        val ordersDeferred = coroutineScope { async { activityService.getTradeOrders(events) } }

        // Creators needed for non-burn transfers to fulfill ownerships
        val itemsRequiredCreators = LinkedHashSet<String>()
        events.forEach { event ->
            when (event) {
                is ImmutablexMint -> itemsRequiredCreators.add(event.itemId())
                is ImmutablexTransfer -> if (!event.isBurn) itemsRequiredCreators.add(event.itemId())
                is ImmutablexTrade -> itemsRequiredCreators.addAll(
                    listOfNotNull(event.make.itemId(), event.take.itemId())
                )
                else -> Unit
            }
        }

        val creators = itemService.getItemCreators(itemsRequiredCreators.toList())
        val orders = ordersDeferred.await()

        events.forEach { event ->
            when (event) {
                is ImmutablexMint -> onMint(event, creators)
                is ImmutablexTransfer -> onTransfer(event, creators)
                is ImmutablexTrade -> onTrade(event, orders, creators)
                else -> Unit
            }
            sendActivity(event, orders)
        }
    }

    private suspend fun sendActivity(event: ImmutablexEvent, orders: Map<Long, ImmutablexOrder>) {
        try {
            val converted = ImxActivityConverter.convert(event, orders)
            activityHandler.onEvent(converted)
        } catch (e: ImxDataException) {
            // It could happen if there is no orders specified in TRADE activity
            // It should not happen on prod, but if there is inconsistent data we can just skip it
            // and then report to IMX support
            logger.error("Failed to process Activity (invalid data), skipped: {}, error: {}", event, e.message)
            markError(event)
        }
    }

    private suspend fun onMint(mint: ImmutablexMint, creators: Map<String, String>) {
        val item = ImxItemConverter.convert(mint, creators[mint.itemId()], blockchain)
        itemHandler.onEvent(UnionItemUpdateEvent(item))

        onItemTransferred(mint.itemId(), null, mint.user, mint.timestamp, creators)
    }

    private suspend fun onTransfer(transfer: ImmutablexTransfer, creators: Map<String, String>) {
        val itemId = transfer.token.data.itemId()
        val receiver = if (transfer.isBurn) null else transfer.receiver

        onItemTransferred(itemId, transfer.user, receiver, transfer.timestamp, creators)

        if (transfer.isBurn) {
            itemHandler.onEvent(UnionItemDeleteEvent(ItemIdDto(blockchain, transfer.encodedItemId())))
        }
    }

    private suspend fun onTrade(
        trade: ImmutablexTrade,
        orders: Map<Long, ImmutablexOrder>,
        creators: Map<String, String>
    ) {
        val maker = orders[trade.make.orderId]?.creator
        val taker = orders[trade.take.orderId]?.creator

        val lostByMakerItemId = trade.take.itemId() // for regular trade
        val lostByTakerItemId = trade.make.itemId() // for swap case

        onItemTransferred(lostByTakerItemId, taker, maker, trade.timestamp, creators)
        onItemTransferred(lostByMakerItemId, maker, taker, trade.timestamp, creators)
    }

    private suspend fun onItemTransferred(
        itemId: String?,
        fromUser: String?,
        toUser: String?,
        date: Instant,
        creators: Map<String, String>
    ) {
        if (itemId == null) {
            return
        }
        if (fromUser != null) {
            val fromUserAddress = UnionAddressConverter.convert(blockchain, fromUser)
            val deletedOwnershipId = OwnershipIdDto(blockchain, itemId, fromUserAddress)
            ownershipHandler.onEvent(UnionOwnershipDeleteEvent(deletedOwnershipId))
        }
        if (toUser != null) {
            val newOwnership = ImxOwnershipConverter.toOwnership(
                blockchain,
                TokenIdDecoder.decodeItemId(itemId),
                toUser,
                creators[itemId],
                date
            )
            ownershipHandler.onEvent(UnionOwnershipUpdateEvent(newOwnership))
        }
    }

    private fun markError(event: ImmutablexEvent) {
        val type = when (event) {
            is ImmutablexTrade -> ImxScanEntityType.TRADE
            is ImmutablexMint -> ImxScanEntityType.MINT
            is ImmutablexTransfer -> ImxScanEntityType.TRANSFER
            is ImmutablexWithdrawal -> ImxScanEntityType.WITHDRAWAL
            is ImmutablexDeposit -> ImxScanEntityType.DEPOSIT
        }
        imxScanMetrics.onEventError(type.name)
    }
}
