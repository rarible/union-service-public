package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthActivityConverter
import org.slf4j.LoggerFactory

abstract class EthActivityEventHandler(
    blockchain: BlockchainDto,
    override val handler: IncomingEventHandler<UnionActivityDto>,
    private val ethActivityConverter: EthActivityConverter
) : AbstractBlockchainEventHandler<com.rarible.protocol.dto.ActivityDto, UnionActivityDto>(
    blockchain,
    EventType.ACTIVITY
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(event: com.rarible.protocol.dto.ActivityDto): UnionActivityDto {
        logger.debug("Received Ethereum ({}) Activity event: type={}", blockchain, event::class.java.simpleName)
        return ethActivityConverter.convert(event, blockchain)
    }
}

open class EthereumActivityEventHandler(
    handler: IncomingEventHandler<UnionActivityDto>, ethActivityConverter: EthActivityConverter
) : EthActivityEventHandler(BlockchainDto.ETHEREUM, handler, ethActivityConverter)

open class PolygonActivityEventHandler(
    handler: IncomingEventHandler<UnionActivityDto>, ethActivityConverter: EthActivityConverter
) : EthActivityEventHandler(BlockchainDto.POLYGON, handler, ethActivityConverter)