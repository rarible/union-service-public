package com.rarible.protocol.union.integration.tezos.dipdup.event

import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.model.UnionOwnershipDeleteEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.core.model.itemId
import com.rarible.protocol.union.core.model.ownershipId
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BurnActivityDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.TransferActivityDto
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktOwnershipService
import org.slf4j.LoggerFactory

open class DipDupTransfersEventHandler(
    private val ownershipHandler: IncomingEventHandler<UnionOwnershipEvent>,
    private val ownershipService: TzktOwnershipService,
    private val tokenService: TzktItemService
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val blockchain = BlockchainDto.TEZOS

    fun isTransfersEvent(event: ActivityDto): Boolean {
        return event is MintActivityDto || event is TransferActivityDto || event is BurnActivityDto
    }

    suspend fun handle(event: ActivityDto) {
        if (isNft(event)) {
            when (event) {
                is MintActivityDto -> {
                    ownershipHandler.onEvent(UnionOwnershipUpdateEvent(getOwnership(event.ownershipId())))
                }
                is TransferActivityDto -> {
                    ownershipHandler.onEvent(UnionOwnershipUpdateEvent(
                        getOwnership(event.itemId()?.toOwnership(event.from.value))
                    ))
                    ownershipHandler.onEvent(UnionOwnershipUpdateEvent(getOwnership(event.ownershipId())))
                }
                is BurnActivityDto -> {
                    ownershipHandler.onEvent(UnionOwnershipUpdateEvent(getOwnership(event.itemId()?.let { it.toOwnership(event.owner.value) })))
                }
            }
        }
    }

    private suspend fun getOwnership(id: OwnershipIdDto?): UnionOwnership {
        return id?.let { ownershipService.getOwnershipById(it.value) } ?: throw RuntimeException("Ownership is empty")
    }

    suspend fun isNft(event: ActivityDto): Boolean {
        return event.itemId()?.let { tokenService.isNft(it.value) } ?: false
    }

}
