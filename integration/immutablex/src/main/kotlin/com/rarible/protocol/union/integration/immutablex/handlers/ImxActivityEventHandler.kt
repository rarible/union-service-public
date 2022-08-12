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
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexDeposit
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexEvent
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexMint
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTrade
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTransfer
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexWithdrawal
import com.rarible.protocol.union.integration.immutablex.converter.ImxDataException
import com.rarible.protocol.union.integration.immutablex.converter.ImxItemConverter
import com.rarible.protocol.union.integration.immutablex.converter.ImxOwnershipConverter
import com.rarible.protocol.union.integration.immutablex.scanner.ImxScanEntityType
import com.rarible.protocol.union.integration.immutablex.scanner.ImxScanMetrics
import com.rarible.protocol.union.integration.immutablex.service.ImxActivityService
import org.slf4j.LoggerFactory

class ImxActivityEventHandler(
    private val activityHandler: IncomingEventHandler<ActivityDto>,
    private val itemHandler: IncomingEventHandler<UnionItemEvent>,
    private val ownershipHandler: IncomingEventHandler<UnionOwnershipEvent>,

    private val activityService: ImxActivityService,
    private val imxScanMetrics: ImxScanMetrics,
) {

    private val blockchain = BlockchainDto.IMMUTABLEX

    private val logger = LoggerFactory.getLogger(javaClass)

    // TODO ideally there should be batch processing
    suspend fun handle(event: ImmutablexEvent) {

        when (event) {
            is ImmutablexMint -> onMint(event)
            is ImmutablexTransfer -> onTransfer(event)
        }

        val converted = try {
            activityService.convert(listOf(event)).first()
        } catch (e: ImxDataException) {
            // It could happen if there is no orders specified in TRADE activity
            // It should not happen on prod, but if there is inconsistent data we can just skip it
            // and then report to IMX support
            logger.error("Failed to process Activity (invalid data), skipped: {}, error: {}", event, e.message)
            markError(event)
            return
        }

        activityHandler.onEvent(converted)
    }

    private suspend fun onMint(mint: ImmutablexMint) {
        val item = ImxItemConverter.convert(mint, blockchain)
        itemHandler.onEvent(UnionItemUpdateEvent(item))
    }

    private suspend fun onTransfer(transfer: ImmutablexTransfer) {
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
            val newOwnership = ImxOwnershipConverter.convert(transfer, null, blockchain)
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
