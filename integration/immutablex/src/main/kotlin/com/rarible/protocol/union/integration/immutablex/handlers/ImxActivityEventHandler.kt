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
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.dto.OrderMatchSwapDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.ext
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexDeposit
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexEvent
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexMint
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrder
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTrade
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTransfer
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexWithdrawal
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
        val itemsRequiredCreators = events.filter { it is ImmutablexTransfer && !it.isBurn }
            .map { (it as ImmutablexTransfer).itemId() }

        val creators = itemService.getItemCreators(itemsRequiredCreators)
        val orders = ordersDeferred.await()

        events.forEach { event ->
            when (event) {
                is ImmutablexMint -> onMint(event)
                is ImmutablexTransfer -> onTransfer(event, creators)
                else -> Unit
            }
            val converted = convertActivity(event, orders)
            when (converted) {
                is OrderMatchSwapDto -> onSwap(converted)
                is OrderMatchSellDto -> onMatch(converted)
                else -> Unit
            }
            converted?.let { activityHandler.onEvent(converted) }
        }
    }

    private fun convertActivity(event: ImmutablexEvent, orders: Map<Long, ImmutablexOrder>): ActivityDto? {
        return try {
            ImxActivityConverter.convert(event, orders)
        } catch (e: ImxDataException) {
            // It could happen if there is no orders specified in TRADE activity
            // It should not happen on prod, but if there is inconsistent data we can just skip it
            // and then report to IMX support
            logger.error("Failed to process Activity (invalid data), skipped: {}, error: {}", event, e.message)
            markError(event)
            null
        }
    }

    private suspend fun onMint(mint: ImmutablexMint) {
        val item = ImxItemConverter.convert(mint, blockchain)
        itemHandler.onEvent(UnionItemUpdateEvent(item))
    }

    private suspend fun onTransfer(transfer: ImmutablexTransfer, creators: Map<String, String>) {
        val deletedOwnershipId = OwnershipIdDto(
            blockchain,
            transfer.token.data.encodedItemId(),
            UnionAddressConverter.convert(blockchain, transfer.user)
        )

        ownershipHandler.onEvent(UnionOwnershipDeleteEvent(deletedOwnershipId))

        // Send burn item event or change ownership
        if (transfer.isBurn) {
            itemHandler.onEvent(UnionItemDeleteEvent(deletedOwnershipId.getItemId()))
        } else {
            val newOwnership = ImxOwnershipConverter.convert(transfer, creators[transfer.itemId()], blockchain)
            ownershipHandler.onEvent(UnionOwnershipUpdateEvent(newOwnership))
        }
    }

    private suspend fun onMatch(activity: OrderMatchSellDto) {
        val deletedOwnershipId = activity.nft.type.ext.itemId!!.toOwnership(activity.seller.value)
        // TODO add creator
        val newOwnership = ImxOwnershipConverter.convert(activity, null, blockchain)

        ownershipHandler.onEvent(UnionOwnershipDeleteEvent(deletedOwnershipId))
        ownershipHandler.onEvent(UnionOwnershipUpdateEvent(newOwnership))
    }

    private suspend fun onSwap(swap: OrderMatchSwapDto) {
        val left = swap.left.asset.type.ext
        val right = swap.right.asset.type.ext
        if (left.isNft) {
            val deletedOwnershipId = left.itemId!!.toOwnership(swap.left.maker.value)
            // TODO add creator
            val newOwnership = ImxOwnershipConverter.convert(swap, swap.right, null, blockchain)
            ownershipHandler.onEvent(UnionOwnershipDeleteEvent(deletedOwnershipId))
            ownershipHandler.onEvent(UnionOwnershipUpdateEvent(newOwnership))
        }
        if (right.isNft) {
            val deletedOwnershipId = right.itemId!!.toOwnership(swap.right.maker.value)
            // TODO add creator
            val newOwnership = ImxOwnershipConverter.convert(swap, swap.left, null, blockchain)
            ownershipHandler.onEvent(UnionOwnershipDeleteEvent(deletedOwnershipId))
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
