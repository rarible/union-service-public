package com.rarible.protocol.union.listener.handler.tezos

import com.rarible.protocol.tezos.dto.OwnershipEventDto
import com.rarible.protocol.union.core.tezos.converter.TezosOwnershipConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import com.rarible.protocol.union.listener.service.EnrichmentOwnershipEventService
import org.slf4j.LoggerFactory

class TezosOwnershipEventHandler(
    private val enrichmentOwnershipEventService: EnrichmentOwnershipEventService,
    private val blockchain: BlockchainDto
) : AbstractEventHandler<OwnershipEventDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: OwnershipEventDto) {
        logger.debug("Received Tezos Ownership event: type={}", event::class.java.simpleName)

        when (event.type) {
            OwnershipEventDto.Type.UPDATE -> {
                val ownership = TezosOwnershipConverter.convert(event.ownership!!, blockchain)
                enrichmentOwnershipEventService.onOwnershipUpdated(ownership)
            }
            OwnershipEventDto.Type.DELETE -> {
                val ownershipId = OwnershipIdDto(
                    blockchain = blockchain,
                    token = UnionAddress(blockchain, event.ownership!!.contract),
                    tokenId = event.ownership!!.tokenId,
                    owner = UnionAddress(blockchain, event.ownership!!.owner)
                )
                enrichmentOwnershipEventService.onOwnershipDeleted(ownershipId)
            }
            OwnershipEventDto.Type.SERIALIZATION_FAILED -> {
                // skip it, will be logged inside of parser
            }
        }

    }

}
