package com.rarible.protocol.union.integration.tezos.dipdup.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.dipdup.listener.model.DipDupCollectionEvent
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.exception.UnionDataFormatException
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionCollectionEvent
import com.rarible.protocol.union.core.model.UnionCollectionUpdateEvent
import com.rarible.protocol.union.core.model.stubEventMark
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupIntegrationProperties
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupCollectionConverter
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktCollectionService
import org.slf4j.LoggerFactory

open class DipDupCollectionEventHandler(
    override val handler: IncomingEventHandler<UnionCollectionEvent>,
    private val tzktCollectionService: TzktCollectionService,
    private val mapper: ObjectMapper,
    private val properties: DipDupIntegrationProperties
) : AbstractBlockchainEventHandler<DipDupCollectionEvent, UnionCollectionEvent>(
    BlockchainDto.TEZOS,
    EventType.COLLECTION
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(event: DipDupCollectionEvent): UnionCollectionEvent? {
        logger.info("Received {} Collection event: {}", blockchain, mapper.writeValueAsString(event))
        return try {
            val collection = DipDupCollectionConverter.convert(event.collection)

            val unionCollectionEvent = if (properties.enrichDipDupCollection) {
                // Enrich by meta fields, lately it's better to move it to the indexer
                val tzktCollection = tzktCollectionService.getCollectionById(event.collection.id, true)
                UnionCollectionUpdateEvent(
                    collection.copy(name = tzktCollection.name, symbol = tzktCollection.symbol),
                    stubEventMark()
                )
            } else {
                UnionCollectionUpdateEvent(collection, stubEventMark())
            }

            unionCollectionEvent
        } catch (e: UnionDataFormatException) {
            logger.warn("DipDup collection event was skipped because wrong data format", e)
            null
        }
    }
}
