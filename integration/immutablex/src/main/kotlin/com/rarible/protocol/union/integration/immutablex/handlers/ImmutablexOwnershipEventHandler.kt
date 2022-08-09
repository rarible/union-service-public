package com.rarible.protocol.union.integration.immutablex.handlers

import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.model.UnionOwnershipDeleteEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexEvent
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTransfer
import java.math.BigInteger

class ImmutablexOwnershipEventHandler(
    override val handler: IncomingEventHandler<UnionOwnershipEvent>
) : AbstractBlockchainEventHandler<ImmutablexEvent, UnionOwnershipEvent>(BlockchainDto.IMMUTABLEX) {
    override suspend fun handle(event: ImmutablexEvent) {
        if (event is ImmutablexTransfer && event.status == "success") {
            handler.onEvent(
                UnionOwnershipDeleteEvent(
                    ownershipId = OwnershipIdDto(
                        blockchain,
                        event.token.data.encodedItemId(),
                        UnionAddressConverter.convert(blockchain, event.user)
                    )
                )
            )
            handler.onEvent(
                UnionOwnershipUpdateEvent(
                    ownership = UnionOwnership(
                        id = OwnershipIdDto(
                            blockchain,
                            event.token.data.encodedItemId(),
                            UnionAddressConverter.convert(blockchain, event.receiver)
                        ),
                        collection = CollectionIdDto(blockchain, event.token.data.tokenAddress),
                        value = event.token.data.quantity,
                        createdAt = event.timestamp,
                        lastUpdatedAt = event.timestamp, // TODO is it correct?
                        lazyValue = BigInteger.ZERO
                    )
                )
            )
        }
    }
}
