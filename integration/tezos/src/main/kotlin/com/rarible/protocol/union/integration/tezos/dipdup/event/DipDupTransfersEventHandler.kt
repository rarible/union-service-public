package com.rarible.protocol.union.integration.tezos.dipdup.event

import com.rarible.protocol.union.core.exception.UnionValidationException
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionActivityDto
import com.rarible.protocol.union.core.model.UnionBurnActivityDto
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.core.model.UnionMintActivityDto
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.model.UnionOwnershipDeleteEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.core.model.UnionTransferActivityDto
import com.rarible.protocol.union.core.model.stubEventMark
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktOwnershipService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import java.math.BigInteger

@Deprecated("Replaced by item/ownership/activity handlers")
open class DipDupTransfersEventHandler(
    private val ownershipHandler: IncomingEventHandler<UnionOwnershipEvent>,
    private val ownershipService: TzktOwnershipService,
    private val itemHandler: IncomingEventHandler<UnionItemEvent>,
    private val tokenService: TzktItemService
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val blockchain = BlockchainDto.TEZOS

    fun isTransfersEvent(event: UnionActivityDto): Boolean {
        return event is UnionMintActivityDto || event is UnionTransferActivityDto || event is UnionBurnActivityDto
    }

    suspend fun handle(event: UnionActivityDto) = coroutineScope {
        if (isNft(event)) {
            when (event) {
                is UnionMintActivityDto -> listOf(
                    async { sendItemEvent(event.itemId()) },
                    async { sendOwnershipEvent(event.ownershipId()) })

                is UnionTransferActivityDto -> listOf(
                    async { sendOwnershipEvent(event.itemId()?.toOwnership(event.from.value)) },
                    async { sendOwnershipEvent(event.ownershipId()) }
                )

                is UnionBurnActivityDto -> listOf(
                    async { sendItemEvent(event.itemId()) },
                    async { sendOwnershipEvent(event.itemId()?.let { it.toOwnership(event.owner.value) }) }
                )

                else -> emptyList()
            }.awaitAll()
        } else {
            logger.debug("Activity is skipped because it's not nft, ItemId: ${event.itemId()}")
        }
    }

    private suspend fun sendOwnershipEvent(ownershipId: OwnershipIdDto?) {
        val ownership = getOwnership(ownershipId)
        if (ownership.value > BigInteger.ZERO) {
            ownershipHandler.onEvent(UnionOwnershipUpdateEvent(ownership, stubEventMark()))
        } else {
            ownershipHandler.onEvent(UnionOwnershipDeleteEvent(ownership.id, stubEventMark()))
        }
    }

    private suspend fun sendItemEvent(itemId: ItemIdDto?) {
        val token = getItem(itemId)
        if (token.supply > BigInteger.ZERO) {
            itemHandler.onEvent(UnionItemUpdateEvent(token, stubEventMark()))
        } else {
            itemHandler.onEvent(UnionItemDeleteEvent(token.id, stubEventMark()))
        }
    }

    private suspend fun getOwnership(id: OwnershipIdDto?): UnionOwnership {
        return id?.let { ownershipService.getOwnershipById(it.value) }
            ?: throw UnionValidationException("Ownership is empty")
    }

    private suspend fun getItem(id: ItemIdDto?): UnionItem {
        return id?.let { tokenService.getItemById(id.value) } ?: throw UnionValidationException("ItemId is empty")
    }

    private suspend fun isNft(event: UnionActivityDto): Boolean {
        return event.itemId()?.let { tokenService.isNft(it.value) } ?: false
    }

}
