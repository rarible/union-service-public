package com.rarible.protocol.union.integration.tezos.dipdup.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.dipdup.client.core.model.DipDupOrder
import com.rarible.dipdup.client.core.model.TezosPlatform
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupIntegrationProperties
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupOrderConverter
import org.slf4j.LoggerFactory

open class DipDupOrderEventHandler(
    override val handler: IncomingEventHandler<UnionOrderEvent>,
    private val dipDupOrderConverter: DipDupOrderConverter,
    private val mapper: ObjectMapper,
    private val marketplaces: DipDupIntegrationProperties.Marketplaces
) : AbstractBlockchainEventHandler<DipDupOrder, UnionOrderEvent>(com.rarible.protocol.union.dto.BlockchainDto.TEZOS) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: DipDupOrder) {
        logger.info("Received DipDup order event: {}", mapper.writeValueAsString(event))
        val next = (setOf(TezosPlatform.RARIBLE_V1, TezosPlatform.RARIBLE_V2).contains(event.platform))
                    || (event.platform == TezosPlatform.HEN && marketplaces.hen)
                    || (event.platform == TezosPlatform.OBJKT_V1 && marketplaces.objkt)
                    || (event.platform == TezosPlatform.OBJKT_V2 && marketplaces.objktV2)
                    || (event.platform == TezosPlatform.VERSUM_V1 && marketplaces.versum)
                    || (event.platform == TezosPlatform.TEIA_V1 && marketplaces.teia)
                    || (event.platform == TezosPlatform.FXHASH_V1 && marketplaces.fxhashV1)
                    || (event.platform == TezosPlatform.FXHASH_V2 && marketplaces.fxhashV2)
        if (next) {
            val unionOrder = dipDupOrderConverter.convert(event, blockchain)
            handler.onEvent(UnionOrderUpdateEvent(unionOrder))
        }
    }

}
