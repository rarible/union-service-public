package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.protocol.dto.EthActivityEventDto
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthActivityConverter
import org.slf4j.LoggerFactory

class EthActivityEventHandler(
    blockchain: BlockchainDto,
    override val handler: IncomingEventHandler<UnionActivity>,
    private val ethActivityConverter: EthActivityConverter
) : AbstractBlockchainEventHandler<EthActivityEventDto, UnionActivity>(
    blockchain,
    EventType.ACTIVITY
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(event: EthActivityEventDto): UnionActivity {
        logger.info(
            "Received {} Activity event: {}:{}",
            blockchain,
            event.activity::class.simpleName,
            event.activity.id
        )
        return ethActivityConverter.convert(event, blockchain)
    }
}
