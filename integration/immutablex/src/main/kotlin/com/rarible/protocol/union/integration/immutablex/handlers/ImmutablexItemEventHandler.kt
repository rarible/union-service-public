package com.rarible.protocol.union.integration.immutablex.handlers

import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.group
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexEvent
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexMint
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexTransfer
import java.math.BigInteger
import scalether.domain.Address

class ImmutablexItemEventHandler(
    override val handler: IncomingEventHandler<UnionItemEvent>,
): AbstractBlockchainEventHandler<ImmutablexEvent, UnionItemEvent>(BlockchainDto.IMMUTABLEX) {

    override suspend fun handle(event: ImmutablexEvent) {
        val itemEvent = when (event) {
            is ImmutablexMint -> {
                val itemId =
                    ItemIdDto(blockchain, event.token.data.tokenAddress, event.token.data.tokenId())
                UnionItemUpdateEvent(
                    itemId = itemId,
                    item = UnionItem(
                        id = itemId,
                        collection = CollectionIdDto(blockchain, event.token.data.tokenAddress),
                        creators = listOf(
                            CreatorDto(
                                account = UnionAddress(blockchain.group(), event.user),
                                value = 1
                            )
                        ),
                        lazySupply = BigInteger.ZERO,
                        supply = BigInteger.ONE,
                        mintedAt = event.timestamp,
                        lastUpdatedAt = event.timestamp,
                        deleted = false
                    )
                )
            }
            is ImmutablexTransfer -> {
                if (event.user == Address.ZERO().hex()) {
                    val itemId =
                        ItemIdDto(blockchain, event.token.data.tokenAddress, event.token.data.tokenId())
                    UnionItemDeleteEvent(itemId)
                } else null

            }
            else -> null
        }

        if (itemEvent != null) {
            handler.onEvent(itemEvent)
        }
    }
}
