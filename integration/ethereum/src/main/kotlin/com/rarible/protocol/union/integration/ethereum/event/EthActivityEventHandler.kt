package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.EthereumComponent
import com.rarible.protocol.union.integration.ethereum.PolygonComponent
import com.rarible.protocol.union.integration.ethereum.converter.EthActivityConverter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

sealed class EthActivityEventHandler(
    blockchain: BlockchainDto,
    override val handler: IncomingEventHandler<ActivityDto>,
    private val ethActivityConverter: EthActivityConverter
) : BlockchainEventHandler<com.rarible.protocol.dto.ActivityDto, ActivityDto>(blockchain) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: com.rarible.protocol.dto.ActivityDto) {
        logger.debug("Received Ethereum ({}) Activity event: type={}", blockchain, event::class.java.simpleName)
        val unionEventDto = ethActivityConverter.convert(event, blockchain)
        handler.onEvent(unionEventDto)
    }
}

@Component
@EthereumComponent
class EthereumActivityEventHandler(
    handler: IncomingEventHandler<ActivityDto>,
    ethActivityConverter: EthActivityConverter
) : EthActivityEventHandler(
    BlockchainDto.ETHEREUM,
    handler,
    ethActivityConverter
)

@Component
@PolygonComponent
class PolygonActivityEventHandler(
    handler: IncomingEventHandler<ActivityDto>,
    ethActivityConverter: EthActivityConverter
) : EthActivityEventHandler(
    BlockchainDto.POLYGON,
    handler,
    ethActivityConverter
)
