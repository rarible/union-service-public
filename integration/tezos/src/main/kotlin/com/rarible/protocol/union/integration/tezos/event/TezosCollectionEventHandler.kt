package com.rarible.protocol.union.integration.tezos.event

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.tezos.dto.TezosCollectionSafeEventDto
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionCollectionUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.CollectionUpdateEventDto
import com.rarible.protocol.union.integration.tezos.converter.TezosCollectionConverter
import org.slf4j.LoggerFactory

open class TezosCollectionEventHandler(
    override val handler: IncomingEventHandler<UnionCollectionEvent>
) : AbstractBlockchainEventHandler<TezosCollectionSafeEventDto, UnionCollectionEvent>(BlockchainDto.TEZOS) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @CaptureTransaction("CollectionEvent#TEZOS")
    override suspend fun handle(event: TezosCollectionSafeEventDto) {
        logger.info("Received {} Collection event: {}", blockchain, event)

        when (event.type) {
            TezosCollectionSafeEventDto.Type.UPDATE -> {
                val collection = TezosCollectionConverter.convert(event.collection!!, blockchain)
                val unionCollectionEvent = UnionCollectionUpdateEvent(collection)
                handler.onEvent(unionCollectionEvent)
            }

            TezosCollectionSafeEventDto.Type.SERIALIZATION_FAILED -> {
                // skip it, will be logged inside of parser
            }
        }
    }
}
