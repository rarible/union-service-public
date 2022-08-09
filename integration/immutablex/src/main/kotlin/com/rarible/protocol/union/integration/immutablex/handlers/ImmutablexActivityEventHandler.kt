package com.rarible.protocol.union.integration.immutablex.handlers

import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.L2DepositActivityDto
import com.rarible.protocol.union.dto.L2WithdrawalActivityDto
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexDeposit
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexEvent
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexMint
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTrade
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTransfer
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexWithdrawal
import com.rarible.protocol.union.integration.immutablex.converter.ImxDataException
import com.rarible.protocol.union.integration.immutablex.scanner.ImxScanEntityType
import com.rarible.protocol.union.integration.immutablex.scanner.ImxScanMetrics
import com.rarible.protocol.union.integration.immutablex.service.ImmutablexActivityService
import org.slf4j.LoggerFactory

class ImmutablexActivityEventHandler(
    override val handler: IncomingEventHandler<ActivityDto>,
    private val activityService: ImmutablexActivityService,
    private val imxScanMetrics: ImxScanMetrics,
) : AbstractBlockchainEventHandler<ImmutablexEvent, ActivityDto>(BlockchainDto.IMMUTABLEX) {

    private val logger = LoggerFactory.getLogger(javaClass)

    // TODO ideally there should be batch processing
    override suspend fun handle(event: ImmutablexEvent) {
        val converted = try {
            activityService.convert(listOf(event))
        } catch (e: ImxDataException) {
            // It could happen if there is no orders specified in TRADE activity
            // TODO IMMUTABLEX originally it should not happen, but if there is inconsistent data we can just skip it
            // and then report to IMX support
            logger.error(
                "Failed to process event due to invalid data, will be skipped: {}, error: {}",
                event, e.message
            )
            val type = when (event) {
                is ImmutablexTrade -> ImxScanEntityType.TRADES
                is ImmutablexMint -> ImxScanEntityType.MINTS
                is ImmutablexTransfer -> ImxScanEntityType.TRANSFERS
                is ImmutablexWithdrawal -> ImxScanEntityType.WITHDRAWALS
                is ImmutablexDeposit -> ImxScanEntityType.DEPOSITS
            }
            imxScanMetrics.onEventError(type.name)
            emptyList()
        }

        converted.map {
            when (it) {
                // We don't need these events ATM
                is L2DepositActivityDto -> return@map
                is L2WithdrawalActivityDto -> return@map
                else -> handler.onEvent(it)
            }
        }
    }
}
