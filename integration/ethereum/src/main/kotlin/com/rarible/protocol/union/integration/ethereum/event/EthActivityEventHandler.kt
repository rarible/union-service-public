package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthActivityConverter
import org.slf4j.LoggerFactory

abstract class EthActivityEventHandler(
    blockchain: BlockchainDto,
    override val handler: IncomingEventHandler<ActivityDto>,
    private val ethActivityConverter: EthActivityConverter
) : AbstractBlockchainEventHandler<com.rarible.protocol.dto.ActivityDto, ActivityDto>(blockchain) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun handleInternal(event: com.rarible.protocol.dto.ActivityDto) {
        logger.debug("Received Ethereum ({}) Activity event: type={}", blockchain, event::class.java.simpleName)
        val unionEventDto = ethActivityConverter.convert(event, blockchain)
        handler.onEvent(unionEventDto)
    }
}

open class EthereumActivityEventHandler(
    handler: IncomingEventHandler<ActivityDto>, ethActivityConverter: EthActivityConverter
) : EthActivityEventHandler(BlockchainDto.ETHEREUM, handler, ethActivityConverter) {
    @CaptureTransaction("ActivityEvent#ETHEREUM")
    override suspend fun handle(event: com.rarible.protocol.dto.ActivityDto) = handleInternal(event)
}

open class PolygonActivityEventHandler(
    handler: IncomingEventHandler<ActivityDto>, ethActivityConverter: EthActivityConverter
) : EthActivityEventHandler(BlockchainDto.POLYGON, handler, ethActivityConverter) {
    @CaptureTransaction("ActivityEvent#POLYGON")
    override suspend fun handle(event: com.rarible.protocol.dto.ActivityDto) = handleInternal(event)
}
