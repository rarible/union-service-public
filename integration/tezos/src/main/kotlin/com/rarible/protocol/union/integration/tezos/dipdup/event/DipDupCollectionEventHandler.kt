package com.rarible.protocol.union.integration.tezos.dipdup.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.dipdup.client.core.model.DipDupCollection
import com.rarible.protocol.union.core.exception.UnionDataFormatException
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionCollectionUpdateEvent
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupCollectionConverter
import org.slf4j.LoggerFactory

open class DipDupCollectionEventHandler(
    override val handler: IncomingEventHandler<UnionCollectionEvent>,
    private val dipDupCollectionConverter: DipDupCollectionConverter,
    private val mapper: ObjectMapper
) : AbstractBlockchainEventHandler<DipDupCollection, UnionCollectionEvent>(com.rarible.protocol.union.dto.BlockchainDto.TEZOS) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: DipDupCollection) {
        logger.info("Received DipDup collection event: {}", mapper.writeValueAsString(event))
        try {
            val collection = dipDupCollectionConverter.convert(event)
            val unionCollectionEvent = UnionCollectionUpdateEvent(collection)
            handler.onEvent(unionCollectionEvent)
        } catch (e: UnionDataFormatException) {
            logger.warn("DipDup collection event was skipped because wrong data format", e)
        }
    }

}
