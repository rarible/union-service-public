package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.protocol.dto.EthActivityEventDto
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthActivityConverter
import org.slf4j.LoggerFactory

abstract class EthActivityEventHandler(
    blockchain: BlockchainDto,
    override val handler: IncomingEventHandler<UnionActivity>,
    private val ethActivityConverter: EthActivityConverter
) : AbstractBlockchainEventHandler<EthActivityEventDto, UnionActivity>(
    blockchain,
    EventType.ACTIVITY
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(event: EthActivityEventDto): UnionActivity {
        logger.info("Received Ethereum ({}) Activity event: type={}", blockchain, event::class.java.simpleName)
        return ethActivityConverter.convert(event, blockchain)
    }
}

open class EthereumActivityEventHandler(
    handler: IncomingEventHandler<UnionActivity>, ethActivityConverter: EthActivityConverter
) : EthActivityEventHandler(BlockchainDto.ETHEREUM, handler, ethActivityConverter)

open class PolygonActivityEventHandler(
    handler: IncomingEventHandler<UnionActivity>, ethActivityConverter: EthActivityConverter
) : EthActivityEventHandler(BlockchainDto.POLYGON, handler, ethActivityConverter)