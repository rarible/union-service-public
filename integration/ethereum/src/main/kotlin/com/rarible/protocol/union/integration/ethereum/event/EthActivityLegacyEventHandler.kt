package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthActivityConverter
import org.slf4j.LoggerFactory

@Deprecated("Use EthActivityEventHandler with time marks")
abstract class EthActivityLegacyEventHandler(
    blockchain: BlockchainDto,
    override val handler: IncomingEventHandler<UnionActivity>,
    private val ethActivityConverter: EthActivityConverter
) : AbstractBlockchainEventHandler<com.rarible.protocol.dto.ActivityDto, UnionActivity>(
    blockchain,
    EventType.ACTIVITY
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(event: com.rarible.protocol.dto.ActivityDto): UnionActivity {
        logger.debug("Received Ethereum ({}) Activity event: type={}", blockchain, event::class.java.simpleName)
        return ethActivityConverter.convert(event, blockchain)
    }
}

@Deprecated("Use EthActivityEventHandler with time marks")
open class EthereumActivityLegacyEventHandler(
    handler: IncomingEventHandler<UnionActivity>, ethActivityConverter: EthActivityConverter
) : EthActivityLegacyEventHandler(BlockchainDto.ETHEREUM, handler, ethActivityConverter)

@Deprecated("Use EthActivityEventHandler with time marks")
open class PolygonActivityLegacyEventHandler(
    handler: IncomingEventHandler<UnionActivity>, ethActivityConverter: EthActivityConverter
) : EthActivityLegacyEventHandler(BlockchainDto.POLYGON, handler, ethActivityConverter)

@Deprecated("Use EthActivityEventHandler with time marks")
open class MantleActivityLegacyEventHandler(
    handler: IncomingEventHandler<UnionActivity>, ethActivityConverter: EthActivityConverter
) : EthActivityLegacyEventHandler(BlockchainDto.MANTLE, handler, ethActivityConverter)
